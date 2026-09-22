package org.opentripplanner.core.framework.transaction.moduletest.candyshop.event;

import org.opentripplanner.core.framework.transaction.moduletest.candyshop.order.OrderRepository;
import org.opentripplanner.core.framework.transaction.moduletest.candyshop.order.model.Order;
import org.opentripplanner.core.model.event.EventHandler;

public class OrderEventHandler implements EventHandler<CustomerOrderDomainEvent, OrderRepository> {

  @Override
  public Class<CustomerOrderDomainEvent> eventType() {
    return CustomerOrderDomainEvent.class;
  }

  @Override
  public void handle(CustomerOrderDomainEvent event, OrderRepository orderRepository) {
    orderRepository.save(new Order(event.orderId(), event.description()));
  }
}
