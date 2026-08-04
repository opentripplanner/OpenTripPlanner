package org.opentripplanner.framework.transaction.moduletest.candyshop.order;

import java.util.Map;
import org.opentripplanner.framework.transaction.moduletest.candyshop.base.AbstractRepository;

public class OrderRepositorySnapshot extends AbstractRepository<Order> {

  public OrderRepositorySnapshot(Map<Integer, Order> aById) {
    super(aById);
  }

  @Override
  public Order save(Order entity) {
    throw new UnsupportedOperationException("ASnapshot is immutable");
  }
}
