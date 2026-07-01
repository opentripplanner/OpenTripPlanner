package org.opentripplanner.framework.transaction.moduletest.candyshop.order;

import java.util.HashMap;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;
import org.opentripplanner.framework.transaction.moduletest.candyshop.base.AbstractRepository;

/**
 * This repository implements the {@link RepositoryLifecycle} interface, but does not create a new
 * mutable repository for each transaction. Instead, it returns the same instance for each
 * transaction and freezes it into a snapshot when the transaction is committed. This does not
 * support atomic commits and rollback in case a task fails, but is more memory efficient since
 * only the freeze action triggers copying the internal data structure.
 */
public class OrderRepository
  extends AbstractRepository<Order>
  implements RepositoryLifecycle<OrderSnapshot, OrderRepository> {

  public OrderRepository() {
    super(new HashMap<>());
  }

  @Override
  public OrderRepository copyOnWrite(OrderSnapshot readOnlySnapshot) {
    return this;
  }

  @Override
  public OrderSnapshot freeze(OrderRepository mutableSnapshot) {
    return new OrderSnapshot(copyOfEntitiesById());
  }
}
