package org.opentripplanner.ext.siri.updater.azure;

import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import uk.org.siri.siri21.ServiceDelivery;

/**
 * @param <C> the update context of the write domain the handler writes to: SIRI-ET messages are
 *            applied in the transit domain, SIRI-SX messages in the alert domain.
 */
public interface SiriAzureMessageHandler<C> {
  void setup(WriteToGraphCallback<C> writeToGraphCallback);

  /**
   * Consume ServiceDelivery and update the otp data model within the graph writer thread.
   *
   * @return A future for the graph updating process. Null if the message can't be handled.
   */
  @Nullable
  Future<?> handleMessage(ServiceDelivery serviceDelivery, String messageId);
}
