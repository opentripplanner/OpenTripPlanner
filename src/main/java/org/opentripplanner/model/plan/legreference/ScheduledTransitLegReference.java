package org.opentripplanner.model.plan.legreference;

import java.time.ZoneId;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;

/**
 * A reference which can be used to rebuild an exact copy of a {@link ScheduledTransitLeg} using the
 * {@Link RoutingService}
 */
public record ScheduledTransitLegReference(
  FeedScopedId tripId,
  ServiceDate serviceDate,
  int fromStopPositionInPattern,
  int toStopPositionInPattern
)
  implements LegReference {
  @Override
  public ScheduledTransitLeg getLeg(RoutingService routingService, TransitService transitService) {
    Trip trip = transitService.getTripForId().get(tripId);

    if (trip == null) {
      return null;
    }

    TripPattern tripPattern = null;
    TimetableSnapshot timetableSnapshot = transitService.getTimetableSnapshot();

    // Check if pattern is changed by real-time updater
    if (timetableSnapshot != null) {
      tripPattern = timetableSnapshot.getLastAddedTripPattern(tripId, serviceDate);
    }

    // Otherwise use scheduled pattern
    if (tripPattern == null) {
      tripPattern = transitService.getPatternForTrip().get(trip);
    }

    // no matching pattern found anywhere
    if (tripPattern == null) {
      return null;
    }

    Timetable timetable = transitService.getTimetableForTripPattern(tripPattern, serviceDate);

    TripTimes tripTimes = timetable.getTripTimes(trip);

    // TODO: What should we have here
    ZoneId timeZone = transitService.getTimeZone().toZoneId();

    int boardingTime = tripTimes.getDepartureTime(fromStopPositionInPattern);
    int alightingTime = tripTimes.getArrivalTime(toStopPositionInPattern);

    return new ScheduledTransitLeg(
      tripTimes,
      tripPattern,
      fromStopPositionInPattern,
      toStopPositionInPattern,
      serviceDate.toZonedDateTime(timeZone, boardingTime),
      serviceDate.toZonedDateTime(timeZone, alightingTime),
      serviceDate.toLocalDate(),
      timeZone,
      null,
      null,
      0, // TODO: What should we have here
      null
    );
  }
}
