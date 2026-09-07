package org.opentripplanner.ext.siri.updater.azure;

import java.time.Duration;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.opentripplanner.updater.AlertRealTimeUpdateContext;
import org.opentripplanner.updater.UpdateIncrementality;
import org.opentripplanner.updater.alert.siri.SiriAlertsUpdateHandler;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatcherCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.ServiceDelivery;

public class SiriAzureSXUpdater implements SiriAzureMessageHandler<AlertRealTimeUpdateContext> {

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private final SiriAlertsUpdateHandler updateHandler;

  private WriteToGraphCallback<AlertRealTimeUpdateContext> saveResultOnGraph;

  public SiriAzureSXUpdater(
    SiriAzureSXUpdaterParameters config,
    @Nullable SiriFuzzyTripMatcherCache siriFuzzyTripMatcherCache
  ) {
    this.updateHandler = new SiriAlertsUpdateHandler(
      config.feedId(),
      Duration.ZERO,
      siriFuzzyTripMatcherCache
    );
  }

  @Override
  public void setup(WriteToGraphCallback<AlertRealTimeUpdateContext> writeToGraphCallback) {
    this.saveResultOnGraph = writeToGraphCallback;
  }

  @Override
  @Nullable
  public Future<?> handleMessage(ServiceDelivery serviceDelivery, String messageId) {
    var sxDeliveries = serviceDelivery.getSituationExchangeDeliveries();
    if (sxDeliveries == null || sxDeliveries.isEmpty()) {
      LOG.info("Empty Siri SX message {}", messageId);
      return null;
    } else {
      // Each message received over the Azure Service Bus, as well as the history fetched at
      // startup, contains only the situations that changed.
      return saveResultOnGraph.execute(context ->
        updateHandler.update(serviceDelivery, UpdateIncrementality.DIFFERENTIAL, context)
      );
    }
  }
}
