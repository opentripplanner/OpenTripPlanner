package org.opentripplanner.ext.siri.updater.azure;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import uk.org.siri.siri20.ServiceDelivery;

public interface SiriAzureMessageHandler {
  void setup(WriteToGraphCallback writeToGraphCallback);

  /**
   * Consume ServiceDelivery
   */
  void handleMessage(ServiceDelivery serviceDelivery, String messageId);

  void processHistory(ServiceDelivery siri);
}
