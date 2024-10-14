package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.metrics.BatchTripUpdateMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update OTP stop timetables from some a GTFS-RT source.
 */
public class PollingTripUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(PollingTripUpdater.class);

  private final GtfsRealtimeTripUpdateSource updateSource;
  private final TimetableSnapshotSource snapshotSource;

  /**
   * Feed id that is used for the trip ids in the TripUpdates
   */
  private final String feedId;

  /**
   * Defines when delays are propagated to previous stops and if these stops are given the NO_DATA
   * flag.
   */
  private final BackwardsDelayPropagationType backwardsDelayPropagationType;
  private final Consumer<UpdateResult> recordMetrics;

  /**
   * Parent update manager. Is used to execute graph writer runnables.
   */
  private WriteToGraphCallback saveResultOnGraph;
  /**
   * Set only if we should attempt to match the trip_id from other data in TripDescriptor
   */
  private final boolean fuzzyTripMatching;

  public PollingTripUpdater(
    PollingTripUpdaterParameters parameters,
    TimetableSnapshotSource snapshotSource
  ) {
    super(parameters);
    // Create update streamer from preferences
    this.feedId = parameters.feedId();
    this.updateSource = new GtfsRealtimeTripUpdateSource(parameters);
    this.backwardsDelayPropagationType = parameters.backwardsDelayPropagationType();
    this.snapshotSource = snapshotSource;
    this.fuzzyTripMatching = parameters.fuzzyTripMatching();

    this.recordMetrics = BatchTripUpdateMetrics.batch(parameters);

    LOG.info(
      "Creating stop time updater running every {} seconds : {}",
      pollingPeriod(),
      updateSource
    );
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
    // Get update lists from update source
    List<TripUpdate> updates = updateSource.getUpdates();
    var incrementality = updateSource.incrementalityOfLastUpdates();

    if (updates != null) {
      // Handle trip updates via graph writer runnable
      TripUpdateGraphWriterRunnable runnable = new TripUpdateGraphWriterRunnable(
        snapshotSource,
        fuzzyTripMatching,
        backwardsDelayPropagationType,
        incrementality,
        updates,
        feedId,
        recordMetrics
      );
      saveResultOnGraph.execute(runnable);
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(this.getClass())
      .addObj("updateSource", updateSource)
      .addStr("feedId", feedId)
      .addBool("fuzzyTripMatching", fuzzyTripMatching)
      .toString();
  }
}
