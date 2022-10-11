package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;

class TripUpdateGraphWriterRunnable implements GraphWriterRunnable {

  /**
   * True iff the list with updates represent all updates that are active right now, i.e. all
   * previous updates should be disregarded
   */
  private final boolean fullDataset;

  /**
   * The list with updates to apply to the graph
   */
  private final List<TripUpdate> updates;

  private final GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

  private final BackwardsDelayPropagationType backwardsDelayPropagationType;

  private final String feedId;
  private final Consumer<UpdateResult> sendMetrics;
  private final TimetableSnapshotSource snapshotSource;

  TripUpdateGraphWriterRunnable(
    TimetableSnapshotSource snapshotSource,
    GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher,
    BackwardsDelayPropagationType backwardsDelayPropagationType,
    boolean fullDataset,
    List<TripUpdate> updates,
    String feedId,
    Consumer<UpdateResult> sendMetrics
  ) {
    this.snapshotSource = snapshotSource;
    this.fuzzyTripMatcher = fuzzyTripMatcher;
    this.backwardsDelayPropagationType = backwardsDelayPropagationType;
    this.fullDataset = fullDataset;
    this.updates = Objects.requireNonNull(updates);
    this.feedId = Objects.requireNonNull(feedId);
    this.sendMetrics = sendMetrics;
  }

  @Override
  public void run(Graph graph, TransitModel transitModel) {
    var result = snapshotSource.applyTripUpdates(
      fuzzyTripMatcher,
      backwardsDelayPropagationType,
      fullDataset,
      updates,
      feedId
    );
    sendMetrics.accept(result);
  }
}
