package org.opentripplanner.ext.siri.updater;

import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.ResultLogger;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.ServiceDelivery;

/**
 * Update OTP stop timetables from some a Siri-ET HTTP sources.
 */
public class SiriETUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(SiriETUpdater.class);
  /**
   * Update streamer
   */
  private final EstimatedTimetableSource updateSource;

  /**
   * Feed id that is used for the trip ids in the TripUpdates
   */
  private final String feedId;
  /**
   * Parent update manager. Is used to execute graph writer runnables.
   */
  protected WriteToGraphCallback saveResultOnGraph;

  private final EstimatedTimetableHandler estimatedTimetableHandler;

  private final Consumer<UpdateResult> recordMetrics;

  public SiriETUpdater(
    SiriETUpdaterParameters config,
    TransitModel transitModel,
    SiriTimetableSnapshotSource timetableSnapshotSource
  ) {
    super(config);
    // Create update streamer from preferences
    this.feedId = config.feedId();

    this.updateSource = new SiriETHttpTripUpdateSource(config.sourceParameters());

    this.blockReadinessUntilInitialized = config.blockReadinessUntilInitialized();

    LOG.info(
      "Creating stop time updater (SIRI ET) running every {} seconds : {}",
      pollingPeriod(),
      updateSource
    );

    estimatedTimetableHandler =
      new EstimatedTimetableHandler(
        timetableSnapshotSource,
        config.fuzzyTripMatching(),
        new DefaultTransitService(transitModel),
        feedId
      );

    recordMetrics = TripUpdateMetrics.streaming(config);
  }

  @Override
  public void setup(WriteToGraphCallback writeToGraphCallback) {
    this.saveResultOnGraph = writeToGraphCallback;
  }

  /**
   * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates, and
   * applies those updates to the graph.
   */
  @Override
  public void runPolling() {
    boolean moreData = false;
    do {
      var updates = updateSource.getUpdates();
      if (updates.isPresent()) {
        var incrementality = updateSource.incrementalityOfLastUpdates();
        ServiceDelivery serviceDelivery = updates.get().getServiceDelivery();
        moreData = Boolean.TRUE.equals(serviceDelivery.isMoreData());
        // Mark this updater as primed after last page of updates. Copy moreData into a final
        // primitive, because the object moreData persists across iterations.
        final boolean markPrimed = !moreData;
        List<EstimatedTimetableDeliveryStructure> etds = serviceDelivery.getEstimatedTimetableDeliveries();
        if (etds != null) {
          saveResultOnGraph.execute((graph, transitModel) -> {
            var result = estimatedTimetableHandler.applyUpdate(etds, incrementality);
            ResultLogger.logUpdateResult(feedId, "siri-et", result);
            recordMetrics.accept(result);
            if (markPrimed) {
              primed = true;
            }
          });
        }
      }
    } while (moreData);
  }

  public String toString() {
    String s = (updateSource == null) ? "NONE" : updateSource.toString();
    return "Polling SIRI ET updater with update source = " + s;
  }
}
