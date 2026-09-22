package org.opentripplanner.framework.transaction.moduletest.candyshop.event;

import org.opentripplanner.framework.event.EventHandler;
import org.opentripplanner.framework.transaction.moduletest.candyshop.customer.CustomerRepository;
import org.opentripplanner.framework.transaction.moduletest.candyshop.customer.model.Customer;

public class CustomerEventHandler
  implements EventHandler<CustomerOrderDomainEvent, CustomerRepository>
{

  @Override
  public Class<CustomerOrderDomainEvent> eventType() {
    return CustomerOrderDomainEvent.class;
  }

  @Override
  public void handle(CustomerOrderDomainEvent event, CustomerRepository customerRepository) {
    customerRepository.save(new Customer(event.customerId(), event.customerName()));
  }
}
