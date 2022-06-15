package org.opentripplanner.routing;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;

public class TripTimesShortHelper {

  public static List<TripTimeOnDate> getTripTimesShort(
    RoutingService routingService,
    TransitService transitService,
    Trip trip,
    ServiceDate serviceDate
  ) {
    final ServiceDay serviceDay = new ServiceDay(
      transitService.getServiceCodes(),
      serviceDate,
      transitService.getCalendarService(),
      trip.getRoute().getAgency().getId()
    );
    Timetable timetable = null;
    TimetableSnapshot timetableSnapshot = transitService.getTimetableSnapshot();
    if (timetableSnapshot != null) {
      // Check if realtime-data is available for trip

      TripPattern pattern = timetableSnapshot.getLastAddedTripPattern(trip.getId(), serviceDate);
      if (pattern == null) {
        pattern = transitService.getPatternForTrip().get(trip);
      }
      timetable = timetableSnapshot.resolve(pattern, serviceDate);

      // If realtime moved pattern back to original trip, fetch it instead
      if (timetable.getTripIndex(trip.getId()) == -1) {
        pattern = transitService.getPatternForTrip().get(trip);
        timetable = timetableSnapshot.resolve(pattern, serviceDate);
      }
    }
    if (timetable == null) {
      timetable = transitService.getPatternForTrip().get(trip).getScheduledTimetable();
    }

    // This check is made here to avoid changing TripTimeShort.fromTripTimes
    TripTimes times = timetable.getTripTimes(trip);
    if (!serviceDay.serviceRunning(times.getServiceCode())) {
      return new ArrayList<>();
    } else {
      return TripTimeOnDate.fromTripTimes(timetable, trip, serviceDay);
    }
  }
}
