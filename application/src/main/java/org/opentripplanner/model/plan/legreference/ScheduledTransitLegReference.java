package org.opentripplanner.model.plan.legreference;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.OptionalInt;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.plan.leg.LegConstructionSupport;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;
import org.opentripplanner.routing.algorithm.mapping.AlertToLegMapper;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.collection.TwoWayLinearSearch;
import org.opentripplanner.utils.lang.ObjectUtils;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A reference which can be used to rebuild an exact copy of a {@link ScheduledTransitLeg} using the
 * {@link org.opentripplanner.routing.api.RoutingService}
 */
public record ScheduledTransitLegReference(
  @Nullable FeedScopedId tripId,
  LocalDate serviceDate,
  int fromStopPositionInPattern,
  int toStopPositionInPattern,
  FeedScopedId fromStopId,

  FeedScopedId toStopId,
  @Nullable FeedScopedId tripOnServiceDateId
)
  implements LegReference {
  private static final Logger LOG = LoggerFactory.getLogger(ScheduledTransitLegReference.class);

  public ScheduledTransitLegReference {
    if (!ObjectUtils.oneOf(tripId, tripOnServiceDateId)) {
      throw new IllegalArgumentException(
        "ScheduledTransitLegReference must contain either a Trip id or a TripOnServiceDate id but not both."
      );
    }

    if (fromStopPositionInPattern >= toStopPositionInPattern) {
      throw new IllegalArgumentException(
        "toStopPositionInPattern must be larger than fromStopPositionInPattern."
      );
    }
  }

  /**
   * Reconstruct a scheduled transit leg from this scheduled transit leg reference.
   * <p>
   * The method attempts to find a leg which starts and ends in the same station of the given stops.
   * The transit model could have been modified between the time the reference is created and the
   * time the transit leg is reconstructed (either because new planned data have been
   * rolled out, or because a realtime update has modified a trip).
   * <p>
   * If there are multiple possibilities, the stop which has the closest stop position in pattern
   * to the given reference is used. However, if the stop is on a different platform and a call
   * at the exact stop is immediately before / after it, the exact stop is used.
   * <p>
   * As examples, if E1 and E2 are in the same station and the reference for an A(0)-E2(2) leg was
   * constructed on a trip which now becomes A-B-C-D-E1-F-G-H-E2, it will become an
   * A(0)-E1(4) leg, however, if it now becomes A-B-C-D-E1-E2, it will become an A(0)-E2(5) leg.
   * <p>
   * If it is not possible to reconstruct the leg, which may be because the trip no longer runs
   * between the stations given, or the trip runs between them in the reverse order, the method
   * returns null.
   * <p>
   * If the referenced trip is based on a TripOnServiceDate (i.e. a TransModel dated service
   * journey), the TripOnServiceDate id is stored in the leg reference instead of the Trip id:
   * A TripOnServiceDate id is meant to be more stable than a Trip id across deliveries of planned
   * data, using it gives a better guarantee to reconstruct correctly the original leg.
   *
   * @return The reconstructed leg usable in the current transit model, {@code null} if it is not
   * possible to reconstruct the leg.
   */
  @Override
  @Nullable
  public ScheduledTransitLeg getLeg(TransitService transitService) {
    Trip trip;
    TripOnServiceDate tripOnServiceDate = null;

    if (tripOnServiceDateId != null) {
      tripOnServiceDate = transitService.getTripOnServiceDate(tripOnServiceDateId);
      if (tripOnServiceDate == null) {
        logInvalidLegRef("trip on service date '{}' not found", tripOnServiceDateId);
        return null;
      }
      if (!tripOnServiceDate.getServiceDate().equals(serviceDate)) {
        logInvalidLegRef(
          "trip on service date '{}' does not run on service date {}",
          tripOnServiceDateId,
          serviceDate
        );
        return null;
      }
      trip = tripOnServiceDate.getTrip();
    } else {
      trip = transitService.getTrip(tripId);
    }
    if (trip == null) {
      logInvalidLegRef("trip '{}' not found", tripId);
      return null;
    }

    TripPattern tripPattern = transitService.findPattern(trip, serviceDate);
    if (tripPattern == null) {
      logInvalidLegRef(
        "trip pattern not found for trip '{}' and service date {}",
        trip.getId(),
        serviceDate
      );
      return null;
    }

    OptionalInt optionalUpdatedFromStopPositionInPattern = findStopPositionInPattern(
      tripPattern,
      fromStopPositionInPattern,
      fromStopId,
      transitService
    );
    OptionalInt optionalUpdatedToStopPositionInPattern = findStopPositionInPattern(
      tripPattern,
      toStopPositionInPattern,
      toStopId,
      transitService
    );

    if (optionalUpdatedFromStopPositionInPattern.isEmpty()) {
      LOG.info(
        "Invalid transit leg reference:" +
        " The referenced from stop at position {} with id '{}' cannot be found" +
        " in trip '{}' and service date {}",
        fromStopPositionInPattern,
        fromStopId,
        tripId,
        serviceDate
      );
      return null;
    }

    if (optionalUpdatedToStopPositionInPattern.isEmpty()) {
      LOG.info(
        "Invalid transit leg reference:" +
        " The referenced to stop at position {} with id '{}' cannot be found" +
        " in trip '{}' and service date {}",
        toStopPositionInPattern,
        toStopId,
        tripId,
        serviceDate
      );
      return null;
    }

    var updatedFromStopPositionInPattern = optionalUpdatedFromStopPositionInPattern.getAsInt();
    var updatedToStopPositionInPattern = optionalUpdatedToStopPositionInPattern.getAsInt();

    if (updatedFromStopPositionInPattern >= updatedToStopPositionInPattern) {
      LOG.info(
        "Invalid transit leg reference:" +
        " The calling order for stops with id '{}' and '{}' is reversed" +
        " in trip '{}' and service date {}",
        fromStopId,
        toStopId,
        tripId,
        serviceDate
      );
      return null;
    }

    Timetable timetable = transitService.findTimetable(tripPattern, serviceDate);
    TripTimes tripTimes = timetable.getTripTimes(trip);

    if (tripTimes == null) {
      logInvalidLegRef(
        "trip times not found for trip '{}' and service date {}",
        trip.getId(),
        serviceDate
      );
      return null;
    }

    if (
      !transitService
        .getServiceCodesRunningForDate(serviceDate)
        .contains(tripTimes.getServiceCode())
    ) {
      logInvalidLegRef("the trip '{}' does not run on service date {}", trip.getId(), serviceDate);
      return null;
    }

    // TODO: What should we have here
    ZoneId timeZone = transitService.getTimeZone();

    int boardingTime = tripTimes.getDepartureTime(updatedFromStopPositionInPattern);
    int alightingTime = tripTimes.getArrivalTime(updatedToStopPositionInPattern);

    ScheduledTransitLeg leg = new ScheduledTransitLegBuilder<>()
      .withTripTimes(tripTimes)
      .withTripPattern(tripPattern)
      .withBoardStopIndexInPattern(updatedFromStopPositionInPattern)
      .withAlightStopIndexInPattern(updatedToStopPositionInPattern)
      .withStartTime(ServiceDateUtils.toZonedDateTime(serviceDate, timeZone, boardingTime))
      .withEndTime(ServiceDateUtils.toZonedDateTime(serviceDate, timeZone, alightingTime))
      .withServiceDate(serviceDate)
      .withTripOnServiceDate(tripOnServiceDate)
      .withZoneId(timeZone)
      .withDistanceMeters(
        LegConstructionSupport.computeDistanceMeters(
          tripPattern,
          updatedFromStopPositionInPattern,
          updatedToStopPositionInPattern
        )
      )
      // TODO: What should we have here
      .withGeneralizedCost(0)
      .build();

    return (ScheduledTransitLeg) new AlertToLegMapper(
      transitService.getTransitAlertService(),
      transitService::findMultiModalStation
    ).decorateWithAlerts(leg, false);
  }

  /**
   * Get the stop position of the given stop id, or another stop in the same station, in the given
   * pattern.
   *
   * @return The match closest to the given stop position in the given pattern, except that if
   * an exact match is next to the same station match of a different platform, the exact match
   * is returned.
   */
  private OptionalInt findStopPositionInPattern(
    TripPattern tripPattern,
    int stopPosition,
    FeedScopedId stopId,
    TransitService transitService
  ) {
    var stop = transitService.getStopLocation(stopId);
    OptionalInt exactMatch = findStopPositionInPattern(tripPattern, stopPosition, s ->
      s.getId().equals(stopId)
    );
    OptionalInt sameStationMatch = findStopPositionInPattern(tripPattern, stopPosition, s ->
      s.isPartOfSameStationAs(stop)
    );

    if (exactMatch.isPresent() && sameStationMatch.isPresent()) {
      var exactPosition = exactMatch.getAsInt();
      var sameStationPosition = sameStationMatch.getAsInt();
      if (Math.abs(exactPosition - sameStationPosition) <= 1) {
        return exactMatch;
      }
      if (
        Math.abs(sameStationPosition - stopPosition) < Math.abs(sameStationPosition - exactPosition)
      ) {
        logMatchForChangedStop(tripPattern, stopPosition, stopId, sameStationPosition);
        return sameStationMatch;
      }
      return exactMatch;
    }

    if (exactMatch.isPresent()) {
      return exactMatch;
    }

    if (sameStationMatch.isPresent()) {
      logMatchForChangedStop(tripPattern, stopPosition, stopId, sameStationMatch.getAsInt());
      return sameStationMatch;
    }

    return OptionalInt.empty();
  }

  private void logMatchForChangedStop(
    TripPattern tripPattern,
    int originalStopPosition,
    FeedScopedId originalStopId,
    int updatedStopPosition
  ) {
    LOG.info(
      "Transit leg reference with modified stop id within the same station: " +
      "The referenced stop at position {} with id '{}' does not match" +
      " the stop id '{}' in trip {} and service date {}",
      originalStopPosition,
      originalStopId,
      tripPattern.getStop(updatedStopPosition).getId(),
      tripId,
      serviceDate
    );
  }

  /**
   * Find the stop position in pattern for a stop closest to the given stop position matching the
   * provided matcher
   */
  private OptionalInt findStopPositionInPattern(
    TripPattern tripPattern,
    int stopPosition,
    Predicate<StopLocation> matcher
  ) {
    return TwoWayLinearSearch.findNearest(stopPosition, 0, tripPattern.numberOfStops(), i ->
      matcher.test(tripPattern.getStops().get(i))
    );
  }

  private void logInvalidLegRef(String message, Object... args) {
    if (LOG.isInfoEnabled()) {
      LOG.info("Invalid transit leg reference: " + message + " for " + this, args);
    }
  }
}
