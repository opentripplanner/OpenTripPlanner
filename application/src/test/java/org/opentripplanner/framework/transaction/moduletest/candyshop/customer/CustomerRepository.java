package org.opentripplanner.framework.transaction.moduletest.candyshop.customer;

import org.opentripplanner.framework.transaction.moduletest.candyshop.customer.model.Customer;

public interface CustomerRepository {
  Customer save(Customer customer);
}
