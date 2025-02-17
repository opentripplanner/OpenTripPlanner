package org.opentripplanner.updater.siri.updater;

import java.util.List;
import org.opentripplanner.updater.RealTimeUpdateContext;
import org.opentripplanner.updater.siri.SiriRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

/**
 * A consumer of estimated timetables that applies the real-time updates to the transit model.
 */
public class EstimatedTimetableHandler {

  private final SiriRealTimeTripUpdateAdapter adapter;
  private final boolean fuzzyTripMatching;
  /**
   * The ID for the static feed to which these real time updates are applied
   */
  private final String feedId;

  public EstimatedTimetableHandler(
    SiriRealTimeTripUpdateAdapter adapter,
    boolean fuzzyTripMatching,
    String feedId
  ) {
    this.adapter = adapter;
    this.fuzzyTripMatching = fuzzyTripMatching;
    this.feedId = feedId;
  }

  /**
   * Apply the update to the transit model.
   */
  public UpdateResult applyUpdate(
    List<EstimatedTimetableDeliveryStructure> estimatedTimetableDeliveries,
    UpdateIncrementality updateMode,
    RealTimeUpdateContext context
  ) {
    return adapter.applyEstimatedTimetable(
      fuzzyTripMatching ? context.siriFuzzyTripMatcher() : null,
      context.entityResolver(feedId),
      feedId,
      updateMode,
      estimatedTimetableDeliveries
    );
  }
}
