package org.opentripplanner.framework.transaction.moduletest.candyshop.order;

import org.opentripplanner.framework.transaction.moduletest.candyshop.order.model.Order;

public interface OrderRepository {
  Order save(Order order);
}
