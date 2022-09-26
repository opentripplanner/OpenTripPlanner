package org.opentripplanner.updater.stoptime;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.WriteToGraphCallback;
import org.opentripplanner.util.OTPFeature;
import org.opentripplanner.util.lang.ToStringBuilder;
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
 * rt.sourceType = gtfs-http
 * rt.url = http://host.tld/path
 * rt.feedId = TA
 * </pre>
 */
public class PollingStoptimeUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(PollingStoptimeUpdater.class);

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
  private final AtomicInteger succesfulCounter;
  private final AtomicInteger failedCounter;
  /**
   * Parent update manager. Is used to execute graph writer runnables.
   */
  private WriteToGraphCallback saveResultOnGraph;
  /**
   * Set only if we should attempt to match the trip_id from other data in TripDescriptor
   */
  private GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

  public PollingStoptimeUpdater(
    PollingStoptimeUpdaterParameters parameters,
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

    this.succesfulCounter = getGauge("trip_updates.successful", parameters);
    this.failedCounter = getGauge("trip_updates.failed", parameters);

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
        this::sendMetrics
      );
      saveResultOnGraph.execute(runnable);
    }
  }

  public void sendMetrics(UpdateResult result) {
    this.succesfulCounter.set(result.successful());
    this.failedCounter.set(result.failed());
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

  private static AtomicInteger getGauge(String name, PollingStoptimeUpdaterParameters parameters) {
    if (OTPFeature.ActuatorAPI.isOn()) {
      var tags = List.of(
        Tag.of("configRef", parameters.getConfigRef()),
        Tag.of("url", parameters.httpSourceParameters().getUrl()),
        Tag.of("feedId", parameters.getFeedId())
      );
      return Metrics.globalRegistry.gauge(name, tags, new AtomicInteger(0));
    } else {
      return new AtomicInteger();
    }
  }

  private static TripUpdateSource createSource(PollingStoptimeUpdaterParameters parameters) {
    switch (parameters.getSourceType()) {
      case GTFS_RT_HTTP:
        return new GtfsRealtimeHttpTripUpdateSource(parameters.httpSourceParameters());
      case GTFS_RT_FILE:
        return new GtfsRealtimeFileTripUpdateSource(parameters.fileSourceParameters());
    }
    throw new IllegalArgumentException(
      "Unknown update streamer source type: " + parameters.getSourceType()
    );
  }
}
