package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.WriteToGraphCallback;
import org.opentripplanner.updater.trip.metrics.BatchTripUpdateMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update OTP stop time tables from some (realtime) source
 * <p>
 * Usage example:
 *
 * <pre>
 * rt.type = stop-time-updater
 * rt.frequencySec = 60
 * rt.url = http://host.tld/path
 * rt.feedId = TA
 * </pre>
 */
public class PollingTripUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(PollingTripUpdater.class);

  private final TripUpdateSource updateSource;
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
  private GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

  public PollingTripUpdater(
    PollingTripUpdaterParameters parameters,
    TransitModel transitModel,
    TimetableSnapshotSource snapshotSource
  ) {
    super(parameters);
    // Create update streamer from preferences
    this.feedId = parameters.getFeedId();
    this.updateSource = createSource(parameters);
    this.backwardsDelayPropagationType = parameters.getBackwardsDelayPropagationType();
    this.snapshotSource = snapshotSource;
    if (parameters.fuzzyTripMatching()) {
      this.fuzzyTripMatcher =
        new GtfsRealtimeFuzzyTripMatcher(new DefaultTransitService(transitModel));
    }

    this.recordMetrics = BatchTripUpdateMetrics.batch(parameters);

    LOG.info(
      "Creating stop time updater running every {} seconds : {}",
      pollingPeriodSeconds(),
      updateSource
    );
  }

  @Override
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
  }

  /**
   * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates, and
   * applies those updates to the graph.
   */
  @Override
  public void runPolling() {
    // Get update lists from update source
    List<TripUpdate> updates = updateSource.getUpdates();
    boolean fullDataset = updateSource.getFullDatasetValueOfLastUpdates();

    if (updates != null) {
      // Handle trip updates via graph writer runnable
      TripUpdateGraphWriterRunnable runnable = new TripUpdateGraphWriterRunnable(
        snapshotSource,
        fuzzyTripMatcher,
        backwardsDelayPropagationType,
        fullDataset,
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
      .addBoolIfTrue("fuzzyTripMatching", fuzzyTripMatcher != null)
      .toString();
  }

  private static TripUpdateSource createSource(PollingTripUpdaterParameters parameters) {
    if (parameters.httpSourceParameters().getUrl() != null) {
      return new GtfsRealtimeHttpTripUpdateSource(parameters.httpSourceParameters());
    } else if (parameters.fileSourceParameters().getFile() != null) {
      return new GtfsRealtimeFileTripUpdateSource(parameters.fileSourceParameters());
    } else {
      throw new IllegalArgumentException(
        "Need either a url or file argument to construct a" +
        PollingTripUpdater.class.getSimpleName()
      );
    }
  }
}
