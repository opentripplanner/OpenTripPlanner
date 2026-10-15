package org.opentripplanner.core.framework.transaction.moduletest.candyshop.customer;

import org.opentripplanner.core.framework.transaction.moduletest.candyshop.customer.model.Customer;

public interface CustomerRepository {
  Customer save(Customer customer);
}
