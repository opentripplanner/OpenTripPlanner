package org.opentripplanner.ext.siri.updater.azure;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import uk.org.siri.siri20.ServiceDelivery;

public interface SiriAzureMessageHandler {
  void setup(WriteToGraphCallback writeToGraphCallback);

  /**
   * Consume Service Bus topic message and implement business logic.
   * @param messageContext The Service Bus processor message context that holds a received message and additional methods to settle the message.
   */
  void handleMessage(ServiceBusReceivedMessageContext messageContext);

  void processHistory(ServiceDelivery siri);
}
