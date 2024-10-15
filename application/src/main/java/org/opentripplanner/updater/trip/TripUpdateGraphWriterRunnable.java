package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.RealTimeUpdateContext;
import org.opentripplanner.updater.spi.UpdateResult;

class TripUpdateGraphWriterRunnable implements GraphWriterRunnable {

  private final UpdateIncrementality updateIncrementality;

  /**
   * The list with updates to apply to the graph
   */
  private final List<TripUpdate> updates;

  private final boolean fuzzyTripMatching;

  private final BackwardsDelayPropagationType backwardsDelayPropagationType;

  private final String feedId;
  private final Consumer<UpdateResult> sendMetrics;
  private final TimetableSnapshotSource snapshotSource;

  TripUpdateGraphWriterRunnable(
    TimetableSnapshotSource snapshotSource,
    boolean fuzzyTripMatching,
    BackwardsDelayPropagationType backwardsDelayPropagationType,
    UpdateIncrementality updateIncrementality,
    List<TripUpdate> updates,
    String feedId,
    Consumer<UpdateResult> sendMetrics
  ) {
    this.snapshotSource = snapshotSource;
    this.fuzzyTripMatching = fuzzyTripMatching;
    this.backwardsDelayPropagationType = backwardsDelayPropagationType;
    this.updateIncrementality = updateIncrementality;
    this.updates = Objects.requireNonNull(updates);
    this.feedId = Objects.requireNonNull(feedId);
    this.sendMetrics = sendMetrics;
  }

  @Override
  public void run(RealTimeUpdateContext context) {
    var result = snapshotSource.applyTripUpdates(
      fuzzyTripMatching ? context.gtfsRealtimeFuzzyTripMatcher() : null,
      backwardsDelayPropagationType,
      updateIncrementality,
      updates,
      feedId
    );
    sendMetrics.accept(result);
  }
}
