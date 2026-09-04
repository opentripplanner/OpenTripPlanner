package org.opentripplanner.framework.transaction.moduletest.candyshop.order.internal;

import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;
import org.opentripplanner.framework.transaction.moduletest.candyshop.order.OrderRepository;
import org.opentripplanner.framework.transaction.moduletest.candyshop.order.OrderService;

/**
 * This lifecycle implements the {@link RepositoryLifecycle} interface, but does not create a new
 * mutable repository for each transaction. Instead, it returns the same instance for each
 * transaction and freezes it into a snapshot when the transaction is committed. This does not
 * support atomic commits and rollback in case a task fails, but is more memory efficient since
 * only the freeze action triggers copying the internal data structure.
 */
public class OrderRepositoryLifecycle
  implements RepositoryLifecycle<OrderService, OrderRepository>
{

  // The one live, mutable repository instance, reused for every transaction - not derived from
  // the published snapshot, since freeze() always returns a decoupled copy of it.
  private final OrderRepository repository;

  public OrderRepositoryLifecycle(OrderRepository repository) {
    this.repository = repository;
  }

  @Override
  public OrderRepository copyOnWrite(OrderService snapshot) {
    return repository;
  }

  @Override
  public OrderService freeze(OrderRepository repository) {
    return ((DefaultOrderRepository) repository).freeze();
  }
}
