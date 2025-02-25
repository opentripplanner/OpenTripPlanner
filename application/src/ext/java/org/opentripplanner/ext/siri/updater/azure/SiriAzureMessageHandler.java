package org.opentripplanner.ext.siri.updater.azure;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import uk.org.siri.siri20.ServiceDelivery;

public interface SiriAzureMessageHandler {
  void setup(WriteToGraphCallback writeToGraphCallback);

  /**
   * Consume ServiceDelivery
   */

  @Nullable
  Future<?> handleMessage(ServiceDelivery serviceDelivery, String messageId);
}
