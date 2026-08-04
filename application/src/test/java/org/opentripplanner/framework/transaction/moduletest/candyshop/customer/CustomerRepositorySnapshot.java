package org.opentripplanner.framework.transaction.moduletest.candyshop.customer;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.transaction.moduletest.candyshop.base.AbstractRepository;

public class CustomerRepositorySnapshot extends AbstractRepository<Customer> {

  CustomerRepositorySnapshot(Map<Integer, Customer> aById) {
    super(aById);
  }

  @Override
  public Customer save(Customer entity) {
    throw new UnsupportedOperationException("ASnapshot is immutable");
  }

  CustomerRepository copyOnWrite() {
    return new CustomerRepository(new HashMap<>(copyOfEntitiesById()));
  }
}
