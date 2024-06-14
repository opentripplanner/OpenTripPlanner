package org.opentripplanner.ext.siri.updater;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.siri.EntityResolver;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

/**
 * A consumer of estimated timetables that applies the real-time updates to the transit model.
 */
public class EstimatedTimetableHandler {

  /**
   * Parent update manager. Is used to execute graph writer runnables.
   */
  private final WriteToGraphCallback saveResultOnGraph;

  private final SiriTimetableSnapshotSource snapshotSource;
  private final SiriFuzzyTripMatcher fuzzyTripMatcher;
  private final EntityResolver entityResolver;
  private final Consumer<UpdateResult> updateResultConsumer;
  /**
   * The ID for the static feed to which these TripUpdates are applied
   */
  private final String feedId;

  public EstimatedTimetableHandler(
    WriteToGraphCallback saveResultOnGraph,
    SiriTimetableSnapshotSource snapshotSource,
    boolean fuzzyMatching,
    TransitService transitService,
    Consumer<UpdateResult> updateResultConsumer,
    String feedId
  ) {
    this.saveResultOnGraph = saveResultOnGraph;
    this.snapshotSource = snapshotSource;
    this.fuzzyTripMatcher = fuzzyMatching ? SiriFuzzyTripMatcher.of(transitService) : null;
    this.entityResolver = new EntityResolver(transitService, feedId);
    this.updateResultConsumer = updateResultConsumer;
    this.feedId = feedId;
  }

  public Future<?> applyUpdate(
    List<EstimatedTimetableDeliveryStructure> estimatedTimetableDeliveries,
    UpdateIncrementality updateMode
  ) {
    return applyUpdate(estimatedTimetableDeliveries, updateMode, () -> {});
  }

  public Future<?> applyUpdate(
    List<EstimatedTimetableDeliveryStructure> estimatedTimetableDeliveries,
    UpdateIncrementality updateMode,
    @Nonnull Runnable onUpdateComplete
  ) {
    return saveResultOnGraph.execute((graph, transitModel) -> {
      var results = snapshotSource.applyEstimatedTimetable(
        fuzzyTripMatcher,
        entityResolver,
        feedId,
        updateMode,
        estimatedTimetableDeliveries
      );

      updateResultConsumer.accept(results);
      onUpdateComplete.run();
    });
  }
}
