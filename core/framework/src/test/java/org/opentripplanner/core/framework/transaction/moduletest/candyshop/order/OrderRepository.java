package org.opentripplanner.core.framework.transaction.moduletest.candyshop.order;

import org.opentripplanner.core.framework.transaction.moduletest.candyshop.order.model.Order;

public interface OrderRepository {
  Order save(Order order);
}
