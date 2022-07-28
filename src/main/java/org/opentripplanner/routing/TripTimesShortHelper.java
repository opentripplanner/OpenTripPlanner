package org.opentripplanner.routing;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.util.time.ServiceDateUtils;

public class TripTimesShortHelper {

  public static List<TripTimeOnDate> getTripTimesShort(
    TransitService transitService,
    Trip trip,
    LocalDate serviceDate
  ) {
    Timetable timetable = null;
    TimetableSnapshot timetableSnapshot = transitService.getTimetableSnapshot();
    if (timetableSnapshot != null) {
      // Check if realtime-data is available for trip

      TripPattern pattern = timetableSnapshot.getLastAddedTripPattern(trip.getId(), serviceDate);
      if (pattern == null) {
        pattern = transitService.getPatternForTrip(trip);
      }
      timetable = timetableSnapshot.resolve(pattern, serviceDate);

      // If realtime moved pattern back to original trip, fetch it instead
      if (timetable.getTripIndex(trip.getId()) == -1) {
        pattern = transitService.getPatternForTrip(trip);
        timetable = timetableSnapshot.resolve(pattern, serviceDate);
      }
    }
    if (timetable == null) {
      timetable = transitService.getPatternForTrip(trip).getScheduledTimetable();
    }

    // This check is made here to avoid changing TripTimeShort.fromTripTimes
    TripTimes times = timetable.getTripTimes(trip);
    if (!transitService.getServicesRunningForDate(serviceDate).contains(times.getServiceCode())) {
      return new ArrayList<>();
    } else {
      Instant midnight = ServiceDateUtils
        .asStartOfService(serviceDate, transitService.getTimeZone())
        .toInstant();
      return TripTimeOnDate.fromTripTimes(timetable, trip, serviceDate, midnight);
    }
  }
}
