package org.opentripplanner.ext.siri.updater.azure;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import uk.org.siri.siri20.ServiceDelivery;

public interface SiriAzureMessageHandler {
  void setup(WriteToGraphCallback writeToGraphCallback);

  /**
   * Consume ServiceDelivery and update the otp data model within the graph writer thread.
   *
   * @return A future for the graph updating process. Null if the message can't be handled.
   */
  @Nullable
  Future<?> handleMessage(ServiceDelivery serviceDelivery, String messageId);
}
