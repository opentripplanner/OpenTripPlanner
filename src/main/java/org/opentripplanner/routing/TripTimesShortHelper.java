package org.opentripplanner.routing;

import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.ArrayList;
import java.util.List;

public class TripTimesShortHelper {
  public static List<TripTimeShort> getTripTimesShort(RoutingService routingService, Trip trip, ServiceDate serviceDate) {
    final ServiceDay serviceDay = new ServiceDay(routingService.getServiceCodes(),
        serviceDate,
        routingService.getCalendarService(),
        trip.getRoute().getAgency().getId()
    );
    Timetable timetable = null;
    TimetableSnapshot timetableSnapshot = routingService.getTimetableSnapshot();
    if (timetableSnapshot != null) {
      // Check if realtime-data is available for trip

      TripPattern pattern = timetableSnapshot.getLastAddedTripPattern(trip.getId(), serviceDate);
      if (pattern == null) {
        pattern = routingService.getPatternForTrip().get(trip);
      }
      timetable = timetableSnapshot.resolve(pattern, serviceDate);
    }
    if (timetable == null) {
      timetable = routingService.getPatternForTrip().get(trip).scheduledTimetable;
    }

    // This check is made here to avoid changing TripTimeShort.fromTripTimes
    TripTimes times = timetable.getTripTimes(timetable.getTripIndex(trip.getId()));
    if (!serviceDay.serviceRunning(times.serviceCode)) {
      return new ArrayList<>();
    }
    else {
      return TripTimeShort.fromTripTimes(timetable, trip, serviceDay);
    }
  }

}
