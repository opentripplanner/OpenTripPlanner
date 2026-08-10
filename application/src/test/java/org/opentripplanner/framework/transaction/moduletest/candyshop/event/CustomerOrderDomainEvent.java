package org.opentripplanner.framework.transaction.moduletest.candyshop.event;

import org.opentripplanner.framework.event.DomainEvent;

public record CustomerOrderDomainEvent(
  int orderId,
  String description,
  int customerId,
  String customerName
) implements DomainEvent {}
