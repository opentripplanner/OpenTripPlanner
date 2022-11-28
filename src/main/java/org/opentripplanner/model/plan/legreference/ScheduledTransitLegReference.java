package org.opentripplanner.model.plan.legreference;

import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.algorithm.mapping.AlertToLegMapper;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;

/**
 * A reference which can be used to rebuild an exact copy of a {@link ScheduledTransitLeg} using the
 * {@Link RoutingService}
 */
public record ScheduledTransitLegReference(
  FeedScopedId tripId,
  LocalDate serviceDate,
  int fromStopPositionInPattern,
  int toStopPositionInPattern
)
  implements LegReference {
  @Override
  public ScheduledTransitLeg getLeg(TransitService transitService) {
    Trip trip = transitService.getTripForId(tripId);

    if (trip == null) {
      return null;
    }

    TripPattern tripPattern = transitService.getPatternForTrip(trip, serviceDate);

    // no matching pattern found anywhere
    if (tripPattern == null) {
      return null;
    }

    Timetable timetable = transitService.getTimetableForTripPattern(tripPattern, serviceDate);

    TripTimes tripTimes = timetable.getTripTimes(trip);

    // TODO: What should we have here
    ZoneId timeZone = transitService.getTimeZone();

    int boardingTime = tripTimes.getDepartureTime(fromStopPositionInPattern);
    int alightingTime = tripTimes.getArrivalTime(toStopPositionInPattern);

    ScheduledTransitLeg leg = new ScheduledTransitLeg(
      tripTimes,
      tripPattern,
      fromStopPositionInPattern,
      toStopPositionInPattern,
      ServiceDateUtils.toZonedDateTime(serviceDate, timeZone, boardingTime),
      ServiceDateUtils.toZonedDateTime(serviceDate, timeZone, alightingTime),
      serviceDate,
      timeZone,
      null,
      null,
      0, // TODO: What should we have here
      null
    );

    new AlertToLegMapper(
      transitService.getTransitAlertService(),
      transitService::getMultiModalStationForStation
    )
      .addTransitAlertsToLeg(leg, false);

    return leg;
  }
}
