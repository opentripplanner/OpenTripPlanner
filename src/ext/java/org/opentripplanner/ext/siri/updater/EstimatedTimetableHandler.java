package org.opentripplanner.ext.siri.updater;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.siri.EntityResolver;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

/**
 * A consumer of estimated timetables that applies the real-time updates to the transit model.
 */
public class EstimatedTimetableHandler {

  private final SiriTimetableSnapshotSource snapshotSource;
  private final SiriFuzzyTripMatcher fuzzyTripMatcher;
  private final EntityResolver entityResolver;
  private final Consumer<UpdateResult> updateResultConsumer;
  /**
   * The ID for the static feed to which these TripUpdates are applied
   */
  private final String feedId;

  public EstimatedTimetableHandler(
    SiriTimetableSnapshotSource snapshotSource,
    boolean fuzzyMatching,
    TransitService transitService,
    Consumer<UpdateResult> updateResultConsumer,
    String feedId
  ) {
    this.snapshotSource = snapshotSource;
    this.fuzzyTripMatcher = fuzzyMatching ? SiriFuzzyTripMatcher.of(transitService) : null;
    this.entityResolver = new EntityResolver(transitService, feedId);
    this.updateResultConsumer = updateResultConsumer;
    this.feedId = feedId;
  }

  /**
   * Apply the update to the transit model.
   * @return a future indicating when the changes are applied.
   */
  public void applyUpdate(
    List<EstimatedTimetableDeliveryStructure> estimatedTimetableDeliveries,
    UpdateIncrementality updateMode
  ) {
    applyUpdate(estimatedTimetableDeliveries, updateMode, () -> {});
  }

  /**
   * Apply the update to the transit model.
   * @param onUpdateComplete callback called after the update has been applied.
   * @return a future indicating when the changes are applied.
   */
  public void applyUpdate(
    List<EstimatedTimetableDeliveryStructure> estimatedTimetableDeliveries,
    UpdateIncrementality updateMode,
    @Nonnull Runnable onUpdateComplete
  ) {
    var results = snapshotSource.applyEstimatedTimetable(
      fuzzyTripMatcher,
      entityResolver,
      feedId,
      updateMode,
      estimatedTimetableDeliveries
    );

    updateResultConsumer.accept(results);
    onUpdateComplete.run();
  }
}
