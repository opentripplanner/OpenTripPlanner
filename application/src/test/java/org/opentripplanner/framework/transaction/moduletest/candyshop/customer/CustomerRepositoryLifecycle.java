package org.opentripplanner.framework.transaction.moduletest.candyshop.customer;

import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;

/**
 * This life-cycle will create a new mutable repository for each transaction and freeze it into a
 * snapshot when the transaction is committed. This supports atomic commits and rollback in case a
 * task fails - after a partial update of the repository. To get atomic-commit, a commit must
 * follow every task.
 */
public class CustomerRepositoryLifecycle
  implements RepositoryLifecycle<CustomerSnapshot, CustomerRepository> {

  @Override
  public CustomerRepository copyOnWrite(CustomerSnapshot snapshot) {
    return snapshot.copyOnWrite();
  }

  @Override
  public CustomerSnapshot freeze(CustomerRepository repository) {
    return repository.freeze();
  }
}
