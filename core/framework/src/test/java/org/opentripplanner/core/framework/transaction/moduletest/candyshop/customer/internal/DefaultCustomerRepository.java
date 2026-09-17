package org.opentripplanner.framework.transaction.moduletest.candyshop.customer.internal;

import java.util.Collection;
import org.opentripplanner.framework.transaction.moduletest.candyshop.base.EntityMap;
import org.opentripplanner.framework.transaction.moduletest.candyshop.customer.CustomerRepository;
import org.opentripplanner.framework.transaction.moduletest.candyshop.customer.CustomerRepositorySnapshot;
import org.opentripplanner.framework.transaction.moduletest.candyshop.customer.model.Customer;

/**
 * The repository implementation implements both the {@link CustomerRepository} and
 * {@link CustomerRepositorySnapshot}. It also contains package private lifecycle methods.
 */
public class DefaultCustomerRepository implements CustomerRepository, CustomerRepositorySnapshot {

  private final EntityMap<Customer> customers;

  DefaultCustomerRepository(EntityMap<Customer> customers) {
    this.customers = customers;
  }

  public static DefaultCustomerRepository of() {
    return new DefaultCustomerRepository(EntityMap.of());
  }

  CustomerRepository copyOnWrite() {
    return new DefaultCustomerRepository(customers.copyOf());
  }

  CustomerRepositorySnapshot freeze() {
    // There is no need to make a copy here, since the repository is not modified after this
    // method is called; the transaction framework guarantees this.
    return this;
  }

  @Override
  public Collection<Integer> listIds() {
    return customers.listIds();
  }

  @Override
  public Customer save(Customer customer) {
    return customers.save(customer);
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + "(" + System.identityHashCode(this) + ")";
  }
}
