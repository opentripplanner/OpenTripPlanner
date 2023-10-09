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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger LOG = LoggerFactory.getLogger(ScheduledTransitLegReference.class);

  /**
   * Reconstruct a scheduled transit leg from this scheduled transit leg reference.
   * Since the transit model could have been modified between the time the reference is created
   * and the time the transit leg is reconstructed (either because new planned data have been
   * rolled out, or because a realtime update has modified a trip),
   * it may not be possible to reconstruct the leg.
   * In this case the method returns null.
   */
  @Override
  public ScheduledTransitLeg getLeg(TransitService transitService) {
    Trip trip = transitService.getTripForId(tripId);
    if (trip == null) {
      LOG.info("Invalid transit leg reference: trip {} not found", tripId);
      return null;
    }

    TripPattern tripPattern = transitService.getPatternForTrip(trip, serviceDate);
    if (tripPattern == null) {
      LOG.info(
        "Invalid transit leg reference: trip pattern not found for trip {} and service date {} ",
        tripId,
        serviceDate
      );
      return null;
    }

    int numStops = tripPattern.numberOfStops();
    if (fromStopPositionInPattern >= numStops || toStopPositionInPattern >= numStops) {
      LOG.info(
        "Invalid transit leg reference: boarding stop {} or alighting stop {} is out of range" +
        " in trip {} and service date {} ({} stops in trip pattern) ",
        fromStopPositionInPattern,
        toStopPositionInPattern,
        tripId,
        serviceDate,
        numStops
      );
      return null;
    }

    Timetable timetable = transitService.getTimetableForTripPattern(tripPattern, serviceDate);
    TripTimes tripTimes = timetable.getTripTimes(trip);

    if (tripTimes == null) {
      LOG.info(
        "Invalid transit leg reference: trip times not found for trip {} and service date {} ",
        tripId,
        serviceDate
      );
      return null;
    }

    if (
      !transitService
        .getServiceCodesRunningForDate(serviceDate)
        .contains(tripTimes.getServiceCode())
    ) {
      LOG.info(
        "Invalid transit leg reference: the trip {} does not run on service date {} ",
        tripId,
        serviceDate
      );
      return null;
    }

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
