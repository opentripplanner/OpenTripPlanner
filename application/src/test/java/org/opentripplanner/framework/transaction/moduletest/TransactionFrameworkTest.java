package org.opentripplanner.framework.transaction.moduletest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.TransactionScope;
import org.opentripplanner.framework.transaction.api.WriteContext;
import org.opentripplanner.framework.transaction.internal.TransactionFactory;
import org.opentripplanner.framework.transaction.moduletest.candyshop.base.AbstractRepository;
import org.opentripplanner.framework.transaction.moduletest.candyshop.base.Entity;
import org.opentripplanner.framework.transaction.moduletest.candyshop.customer.Customer;
import org.opentripplanner.framework.transaction.moduletest.candyshop.customer.CustomerRepository;
import org.opentripplanner.framework.transaction.moduletest.candyshop.customer.CustomerRepositoryLifecycle;
import org.opentripplanner.framework.transaction.moduletest.candyshop.customer.CustomerSnapshot;
import org.opentripplanner.framework.transaction.moduletest.candyshop.event.CustomerEventHandler;
import org.opentripplanner.framework.transaction.moduletest.candyshop.event.CustomerOrderDomainEvent;
import org.opentripplanner.framework.transaction.moduletest.candyshop.event.OrderEventHandler;
import org.opentripplanner.framework.transaction.moduletest.candyshop.order.Order;
import org.opentripplanner.framework.transaction.moduletest.candyshop.order.OrderRepository;
import org.opentripplanner.framework.transaction.moduletest.candyshop.order.OrderSnapshot;

/**
 * This test demonstrates how the snapshot framework can be used with two repositories. The example
 * data is a very simple candy shop with a customer and order repositories. There is a difference
 * between the two transaction strategies:
 * <ul>
 *    <li>
 *      CustomerRepository uses copy-on-write and a lifecycle manager, so it supports atomic
 *      commits and rollback on failure.
 *    </li>
 *    <li>
 *      OrderRepository returns the same mutable instance for each transaction and freezes it into
 *      a snapshot on commit. It does not support atomic commits or rollback on failure, but is
 *      more memory efficient, like OTP. It is more efficient since it only copies the internal
 *      data structure on commit.
 *    </li>
 * </ul>
 * The test also demonstrates the transaction visibility contract: state changes are not visible
 * to other tasks until the transaction commits, and the transaction scope is reflected in the
 * published snapshot's toString() for easy debugging.
 */
public class TransactionFrameworkTest {

  private static final TestEvent PIPPI = new TestEvent(1, "Pippi", "pancakes");
  private static final TestEvent TOMMY = new TestEvent(3, "Tommy", "ice cream");
  private static final TestEvent ANNIKA = new TestEvent(7, "Annika", "candy");
  private static final String SCOPE_TXN_1 = "Scope(TXN-1)";
  private static final String SCOPE_TXN_2 = "Scope(TXN-2)";
  private static final String SCOPE_TXN_3 = "Scope(TXN-3)";
  private static final int AWAIT_STATE_TIMEOUT = 2_000;
  private static final int AWAIT_STATE_CHECK_INTERVAL_MS = 10;
  public static final int AUTO_COMMIT_INTERVAL_MS = 50;

  private RepositoryRegistry registry;
  private UpdateManager updateManager;
  private RepositoryHandle<CustomerSnapshot, CustomerRepository> customerRepoHandler;
  private RepositoryHandle<OrderSnapshot, OrderRepository> orderRepoHandler;
  private final List<TestEvent> eventLog = new ArrayList<>();

  @BeforeEach
  public void setUp() throws Exception {
    CustomerRepository customerRepository = new CustomerRepository();
    OrderRepository orderRepository = new OrderRepository();

    // Add PIPPI as initial/static data
    customerRepository.save(PIPPI.customer());
    orderRepository.save(PIPPI.order());

    this.registry = TransactionFactory.createRepositoryRegistry();
    this.customerRepoHandler = registry.registerRepository(
      customerRepository,
      new CustomerRepositoryLifecycle()
    );
    this.orderRepoHandler = registry.registerRepositorySnapshot(
      orderRepository.freeze(orderRepository),
      orderRepository
    );
  }

  private void setupUpdateManagerWithAutoCommits() {
    var threadFactory = new ThreadFactoryBuilder().setNameFormat("AutoCommit").build();
    updateManager = TransactionFactory.createUpdateManagerWithAtomicCommits(
      getClass().getSimpleName(),
      registry,
      threadFactory
    );
    updateManager.register(new CustomerEventHandler(), customerRepoHandler);
    updateManager.register(new OrderEventHandler(), orderRepoHandler);
  }

  /**
   * Use auto-commit and a CountDownLatch to deterministically verify that state is not visible
   * until after the task (and its atomic commit) completes.
   */
  @Test
  public void testHappyDayScenario() throws ExecutionException, InterruptedException {
    setupUpdateManagerWithAutoCommits();
    assertState(SCOPE_TXN_1, PIPPI);

    // Block the task via a latch so the main thread can assert the "before" state without racing.
    var blockTask = new CountDownLatch(1);
    var f = updateManager.submit(c -> {
      publishNewDomainEvent(c, TOMMY);
      awaitUninterruptibly(blockTask);
    });
    // Task is blocked on the latch – state is guaranteed to be unchanged.
    assertState(SCOPE_TXN_1, PIPPI);
    blockTask.countDown();
    f.get();
    assertState(SCOPE_TXN_2, PIPPI, TOMMY);

    f = updateManager.submit(c -> publishNewDomainEvent(c, ANNIKA));
    f.get();
    assertState(SCOPE_TXN_3, PIPPI, TOMMY, ANNIKA);

    updateManager.shutdown();

    assertEventLog(TOMMY, ANNIKA);
  }

  private void setupUpdateManagerWithPeriodicCommits() {
    var threadFactory = new ThreadFactoryBuilder().setNameFormat("AutoCommit").build();
    updateManager = TransactionFactory.createUpdateManagerWithPeriodicCommits(
      getClass().getSimpleName(),
      registry,
      threadFactory,
      Duration.ofMillis(AUTO_COMMIT_INTERVAL_MS)
    );
    updateManager.register(new CustomerEventHandler(), customerRepoHandler);
    updateManager.register(new OrderEventHandler(), orderRepoHandler);
  }

  @Test
  public void testPeriodicCommits() throws ExecutionException, InterruptedException {
    setupUpdateManagerWithPeriodicCommits();
    assertState(SCOPE_TXN_1, PIPPI);

    // Task completes before the periodic scheduler fires its first commit.
    var f = updateManager.submit(c -> publishUsingRepositories(c, TOMMY));
    f.get();
    awaitState(SCOPE_TXN_2, PIPPI, TOMMY);

    f = updateManager.submit(c -> publishNewDomainEvent(c, ANNIKA));
    f.get();
    awaitState(SCOPE_TXN_3, PIPPI, TOMMY, ANNIKA);

    updateManager.shutdown();
    assertEventLog(TOMMY, ANNIKA);
  }

  /**
   * Demonstrates the rollback contract difference between the two lifecycle strategies:
   * <ul>
   *   <li>CustomerRepository uses copy-on-write via {@code ARepositoryLifecycle}: rollback
   *       discards the in-progress copy, so the next task starts from the last committed
   *       snapshot.</li>
   *   <li>OrderRepository returns {@code this} from {@code copyOnWrite}, so mutations written
   *       before the failure are NOT discarded by rollback and leak into the next committed
   *       snapshot.</li>
   * </ul>
   */
  @Test
  public void testRollback() throws ExecutionException, InterruptedException {
    setupUpdateManagerWithAutoCommits();
    assertState(SCOPE_TXN_1, PIPPI);

    // Task writes to both repos and then throws; the framework rolls back and propagates the error.
    var failing = updateManager.submit(c -> {
      publishNewDomainEvent(c, TOMMY);
      throw new RuntimeException("task failed");
    });

    var ex = assertThrows(ExecutionException.class, failing::get);
    assertInstanceOf(RuntimeException.class, ex.getCause());

    // No commit happened – published snapshot is still TXN-1 for both repos.
    assertState(SCOPE_TXN_1, PIPPI);

    // Now submit a succeeding task to reveal the lifecycle difference.
    var succeeding = updateManager.submit(c -> publishUsingRepositories(c, ANNIKA));
    succeeding.get();

    // Customer repo rolled back cleanly: only the initial customer: PIPPI(1) & TOMMY(7).
    // Order did NOT roll back: OderRepository.copyOnWrite returns the same mutable instance, so
    // its state survives reset(). The order ANNIKA(13) leaked into the committed snapshot.
    assertState(SCOPE_TXN_2, List.of(PIPPI, ANNIKA), List.of(PIPPI, TOMMY, ANNIKA));

    updateManager.shutdown();
  }

  private void publishNewDomainEvent(WriteContext writeContext, TestEvent event) {
    eventLog.add(event);
    writeContext.publish(event.domainEvent());
  }

  private void publishUsingRepositories(WriteContext writeContext, TestEvent event) {
    eventLog.add(event);
    writeContext.repository(customerRepoHandler).save(event.customer());
    writeContext.repository(orderRepoHandler).save(event.order());
  }

  private void assertEventLog(TestEvent... expectedEvents) {
    assertEquals(Arrays.asList(expectedEvents), eventLog);
  }

  /// Assert both repositories contains the entities from the given events
  private void assertState(String expScope, TestEvent... expectedEntities) {
    var expected = Arrays.asList(expectedEntities);
    assertState(expScope, expected, expected);
  }

  private void assertState(
    String expScope,
    List<TestEvent> expCustomers,
    List<TestEvent> expOrders
  ) {
    TransactionScope scope = registry.scope();
    assertEquals(expScope, scope.toString());

    assertEntities(expCustomers, TestEvent::customerId, scope, customerRepoHandler);
    assertEntities(expOrders, TestEvent::orderId, scope, orderRepoHandler);
  }

  private <E extends Entity, S extends AbstractRepository<E>> void assertEntities(
    List<TestEvent> expEvents,
    Function<TestEvent, Integer> getId,
    TransactionScope scope,
    RepositoryHandle<S, ?> handle
  ) {
    S snapshot = handle.repositorySnapshot(scope);
    var ids = snapshot.listIds().stream().sorted().toList();
    var expIds = expEvents.stream().map(getId).sorted().toList();
    assertEquals(expIds, ids);
  }

  @SuppressWarnings("BusyWait")
  private void awaitState(String expScope, TestEvent... expected) throws InterruptedException {
    long deadline = System.currentTimeMillis() + AWAIT_STATE_TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      try {
        assertState(expScope, expected);
        return;
      } catch (AssertionError ignored) {
        Thread.sleep(AWAIT_STATE_CHECK_INTERVAL_MS);
      }
    }
    assertState(expScope, expected);
  }

  private static void awaitUninterruptibly(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  record TestEvent(int customerId, String customerName, String orderDescription) {
    private int orderId() {
      return customerId + 10;
    }

    Customer customer() {
      return new Customer(customerId, customerName);
    }

    Order order() {
      return new Order(orderId(), orderDescription);
    }

    CustomerOrderDomainEvent domainEvent() {
      return new CustomerOrderDomainEvent(orderId(), orderDescription, customerId, customerName);
    }

    @Override
    public String toString() {
      return customerName + " order " + orderDescription;
    }
  }
}
