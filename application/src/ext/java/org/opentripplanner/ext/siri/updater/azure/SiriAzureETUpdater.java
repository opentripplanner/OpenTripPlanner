package org.opentripplanner.ext.siri.updater.azure;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.opentripplanner.updater.siri.SiriRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.spi.ResultLogger;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.ServiceDelivery;

public class SiriAzureETUpdater implements SiriAzureMessageHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SiriAzureSXUpdater.class);

  private final SiriRealTimeTripUpdateAdapter adapter;
  private final Consumer<UpdateResult> recordMetrics;
  private final boolean fuzzyTripMatching;
  private final String feedId;

  private WriteToGraphCallback writeToGraphCallback;

  public SiriAzureETUpdater(
    SiriAzureETUpdaterParameters config,
    SiriRealTimeTripUpdateAdapter adapter
  ) {
    this.adapter = adapter;
    this.recordMetrics = TripUpdateMetrics.streaming(config);
    this.fuzzyTripMatching = config.isFuzzyTripMatching();
    this.feedId = Objects.requireNonNull(config.feedId(), "feedId must not be null");
  }

  @Override
  public void setup(WriteToGraphCallback writeToGraphCallback) {
    this.writeToGraphCallback = writeToGraphCallback;
  }

  @Override
  public void handleMessage(ServiceDelivery serviceDelivery, String messageId) {
    var etDeliveries = serviceDelivery.getEstimatedTimetableDeliveries();
    if (etDeliveries == null || etDeliveries.isEmpty()) {
      LOG.info("Empty Siri ET message {}", messageId);
    } else {
      processMessage(etDeliveries);
    }
  }

  private Future<?> processMessage(List<EstimatedTimetableDeliveryStructure> updates) {
    return writeToGraphCallback.execute(context -> {
      var result = adapter.applyEstimatedTimetable(
        fuzzyTripMatching ? context.siriFuzzyTripMatcher() : null,
        context.entityResolver(feedId),
        feedId,
        UpdateIncrementality.DIFFERENTIAL,
        updates
      );
      ResultLogger.logUpdateResultErrors(feedId, "siri-et", result);
      recordMetrics.accept(result);
    });
  }

  @Override
  public void processHistory(ServiceDelivery siri) {
    var updates = siri.getEstimatedTimetableDeliveries();

    if (updates == null || updates.isEmpty()) {
      LOG.info("Did not receive any ET messages from history endpoint");
      return;
    }

    try {
      long t1 = System.currentTimeMillis();
      var f = processMessage(updates);
      f.get();
      LOG.info("Azure ET updater initialized in {} ms.", (System.currentTimeMillis() - t1));
    } catch (ExecutionException | InterruptedException e) {
      throw new SiriAzureInitializationException("Error applying history", e);
    }
  }
}
