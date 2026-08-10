package org.opentripplanner.updater.trip.siri.updater;

import java.util.List;
import org.opentripplanner.updater.RealTimeUpdateContext;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.siri.SiriRealTimeTripUpdateAdapter;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;

/**
 * A consumer of estimated timetables that applies the real-time updates to the transit model.
 */
public class EstimatedTimetableHandler {

  private final SiriRealTimeTripUpdateAdapter adapter;
  /**
   * The ID for the static feed to which these real time updates are applied
   */
  private final String feedId;

  public EstimatedTimetableHandler(SiriRealTimeTripUpdateAdapter adapter, String feedId) {
    this.adapter = adapter;
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
    return adapter
      .forUpdate(context.timetableRepository())
      .applyEstimatedTimetable(
        context.entityResolver(feedId),
        feedId,
        updateMode,
        estimatedTimetableDeliveries
      );
  }
}
