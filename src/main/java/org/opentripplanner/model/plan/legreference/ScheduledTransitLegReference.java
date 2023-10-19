package org.opentripplanner.model.plan.legreference;

import java.time.LocalDate;
import java.time.ZoneId;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.algorithm.mapping.AlertToLegMapper;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A reference which can be used to rebuild an exact copy of a {@link ScheduledTransitLeg} using the
 * {@link org.opentripplanner.routing.api.RoutingService}
 */
public record ScheduledTransitLegReference(
  FeedScopedId tripId,
  LocalDate serviceDate,
  int fromStopPositionInPattern,
  int toStopPositionInPattern,
  FeedScopedId fromStopId,

  FeedScopedId toStopId
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
   * The method checks that the referenced stop positions still refer to the same stop ids.
   * As an exception, the reference is still considered valid if the referenced stop is different
   * but belongs to the same parent station: this covers for example the case of a last-minute
   * platform change in a train station that typically does not affect the validity of the leg.
   */
  @Override
  @Nullable
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

    if (
      !matchReferencedStopInPattern(
        tripPattern,
        fromStopPositionInPattern,
        fromStopId,
        transitService
      ) ||
      !matchReferencedStopInPattern(tripPattern, toStopPositionInPattern, toStopId, transitService)
    ) {
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

  /**
   * Return false if the stop id in the reference does not match the actual stop id in the trip
   * pattern.
   * Return true in the specific case where the stop ids differ, but belong to the same parent
   * station.
   *
   */
  private boolean matchReferencedStopInPattern(
    TripPattern tripPattern,
    int stopPosition,
    FeedScopedId stopId,
    TransitService transitService
  ) {
    if (stopId == null) {
      // this is a legacy reference, skip validation
      // TODO: remove backward-compatible logic after OTP release 2.6
      return true;
    }

    StopLocation stopLocationInPattern = tripPattern.getStops().get(stopPosition);
    if (stopId.equals(stopLocationInPattern.getId())) {
      return true;
    }
    StopLocation stopLocationInLegReference = transitService.getStopLocation(stopId);
    if (
      stopLocationInLegReference == null ||
      stopLocationInPattern.getParentStation() == null ||
      !stopLocationInPattern
        .getParentStation()
        .equals(stopLocationInLegReference.getParentStation())
    ) {
      LOG.info(
        "Invalid transit leg reference:" +
        " The referenced stop at position {} with id '{}' does not match" +
        " the stop id '{}' in trip {} and service date {}",
        stopPosition,
        stopId,
        stopLocationInPattern.getId(),
        tripId,
        serviceDate
      );
      return false;
    }
    LOG.info(
      "Transit leg reference with modified stop id within the same station: " +
      "The referenced stop at position {} with id '{}' does not match\" +\n" +
      "        \" the stop id '{}' in trip {} and service date {}",
      stopPosition,
      stopId,
      stopLocationInPattern.getId(),
      tripId,
      serviceDate
    );
    return true;
  }
}
