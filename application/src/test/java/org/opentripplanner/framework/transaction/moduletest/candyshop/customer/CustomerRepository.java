package org.opentripplanner.framework.transaction.moduletest.candyshop.customer;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.transaction.moduletest.candyshop.base.AbstractRepository;

public class CustomerRepository extends AbstractRepository<Customer> {

  public CustomerRepository(Map<Integer, Customer> entitiesById) {
    super(entitiesById);
  }

  public CustomerRepository() {
    this(new HashMap<>());
  }

  CustomerSnapshot freeze() {
    return new CustomerSnapshot(copyOfEntitiesById());
  }
}
