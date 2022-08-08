package org.opentripplanner.routing;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.util.time.ServiceDateUtils;

public class TripTimesShortHelper {

  public static List<TripTimeOnDate> getTripTimesShort(
    TransitService transitService,
    Trip trip,
    LocalDate serviceDate
  ) {
    // Check if realtime-data changed pattern for trip, otherwise use original
    TripPattern pattern = transitService.getRealtimeAddedTripPattern(trip.getId(), serviceDate);
    if (pattern == null) {
      pattern = transitService.getPatternForTrip(trip);
    }

    Timetable timetable = transitService.getTimetableForTripPattern(pattern, serviceDate);

    // If realtime moved pattern back to original trip, fetch it instead
    if (timetable.getTripIndex(trip.getId()) == -1) {
      pattern = transitService.getPatternForTrip(trip);
      timetable = transitService.getTimetableForTripPattern(pattern, serviceDate);
    }

    // This check is made here to avoid changing TripTimeShort.fromTripTimes
    TripTimes times = timetable.getTripTimes(trip);
    if (
      !transitService.getServiceCodesRunningForDate(serviceDate).contains(times.getServiceCode())
    ) {
      return new ArrayList<>();
    } else {
      Instant midnight = ServiceDateUtils
        .asStartOfService(serviceDate, transitService.getTimeZone())
        .toInstant();
      return TripTimeOnDate.fromTripTimes(timetable, trip, serviceDate, midnight);
    }
  }
}
