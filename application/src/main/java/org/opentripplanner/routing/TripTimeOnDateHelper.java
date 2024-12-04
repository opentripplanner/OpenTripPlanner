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
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TripTimeOnDateHelper {

  private static final Logger LOG = LoggerFactory.getLogger(TripTimeOnDateHelper.class);

  public static List<TripTimeOnDate> getTripTimeOnDates(
    TransitService transitService,
    Trip trip,
    LocalDate serviceDate
  ) {
    TripPattern pattern = transitService.findPattern(trip, serviceDate);

    Timetable timetable = transitService.findTimetable(pattern, serviceDate);

    // If realtime moved pattern back to original trip, fetch it instead
    if (timetable.getTripIndex(trip.getId()) == -1) {
      LOG.warn(
        "Trip {} not found in realtime pattern. This should not happen, and indicates a bug.",
        trip
      );
      pattern = transitService.findPattern(trip);
      timetable = transitService.findTimetable(pattern, serviceDate);
    }

    // This check is made here to avoid changing TripTimeOnDate.fromTripTimes
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
