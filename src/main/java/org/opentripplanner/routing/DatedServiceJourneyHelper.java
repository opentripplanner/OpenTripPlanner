package org.opentripplanner.routing;

import java.time.LocalDate;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripIdAndServiceDate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.service.TransitService;

public class DatedServiceJourneyHelper {

  /**
   * Gets a TripOnServiceDate from a timetable snapshot if it has realtime updates otherwise from
   * the graph index
   */
  public static TripOnServiceDate getTripOnServiceDate(
    TransitService transitService,
    FeedScopedId tripId,
    LocalDate serviceDate
  ) {
    var tuple = new TripIdAndServiceDate(tripId, serviceDate);
    if (transitService.getTimetableSnapshot() != null) {
      if (
        transitService
          .getTimetableSnapshot()
          .getLastAddedTripOnServiceDateByTripIdAndServiceDate()
          .containsKey(tuple)
      ) {
        return transitService
          .getTimetableSnapshot()
          .getLastAddedTripOnServiceDateByTripIdAndServiceDate()
          .get(tuple);
      }
    }
    return transitService.getTripOnServiceDateForTripAndDay(tuple);
  }

  /**
   * Gets a TripOnServiceDate from a timetable snapshot if it has realtime updates otherwise from
   * the graph index
   */
  public static TripOnServiceDate getTripOnServiceDate(
    TransitService transitService,
    FeedScopedId datedServiceJourneyId
  ) {
    TimetableSnapshot timetableSnapshot = transitService.getTimetableSnapshot();
    if (
      timetableSnapshot != null &&
      timetableSnapshot.getLastAddedTripOnServiceDate().containsKey(datedServiceJourneyId)
    ) {
      return timetableSnapshot.getLastAddedTripOnServiceDate().get(datedServiceJourneyId);
    }

    return transitService.getTripOnServiceDateById(datedServiceJourneyId);
  }
}
