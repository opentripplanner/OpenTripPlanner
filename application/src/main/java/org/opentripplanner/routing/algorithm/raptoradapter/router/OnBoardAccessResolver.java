package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingOnBoardAccess;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.OnBoardTripPatternSearch;
import org.opentripplanner.routing.api.request.TripLocation;
import org.opentripplanner.routing.api.request.TripOnDateReference;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * Resolves a {@link TripLocation} into a {@link RoutingOnBoardAccess} by looking up the trip,
 * pattern, stop position, and trip schedule index in the transit model.
 */
public class OnBoardAccessResolver {

  private final TransitService transitService;

  public OnBoardAccessResolver(TransitService transitService) {
    this.transitService = transitService;
  }

  /**
   * Resolve the given trip location to a {@link RoutingOnBoardAccess} containing the on-board
   * access and the service date.
   *
   * @param tripLocation the on-board location to resolve
   * @param patternSearch the search service for the active Raptor pattern index
   * @return a {@link RoutingOnBoardAccess} for the given trip location
   * @throws IllegalArgumentException if the trip, stop, or departure time is invalid
   * @throws RoutingValidationException if the on-board location is ambiguous
   */
  public RoutingOnBoardAccess resolve(
    TripLocation tripLocation,
    OnBoardTripPatternSearch patternSearch
  ) {
    var tripAndServiceDate = resolveTripAndServiceDate(tripLocation.tripOnDateReference());
    var trip = tripAndServiceDate.trip();
    var serviceDate = tripAndServiceDate.serviceDate();
    var tripPattern = findPatternInRaptorData(trip, serviceDate, patternSearch);

    Integer targetSeconds = toSecondsSinceStartOfService(
      tripLocation.aimedDepartureTime(),
      serviceDate
    );
    int stopPosInPattern = findStopPosition(
      tripPattern,
      tripLocation.stopLocationId(),
      trip,
      serviceDate,
      targetSeconds
    );

    RoutingTripPattern routingPattern = tripPattern.getRoutingTripPattern();
    int routeIndex = routingPattern.patternIndex();
    int raptorStopIndex = routingPattern.stopIndex(stopPosInPattern);

    var tripPatternForDates = patternSearch.findTripPatternForDates(routeIndex);
    int tripScheduleIndex = patternSearch.findTripScheduleIndex(
      tripPatternForDates,
      trip,
      serviceDate
    );

    var tripTimes = transitService
      .findTimetable(tripPattern, serviceDate)
      .getTripTimesWithScheduleFallback(trip);
    if (tripTimes == null) {
      throw new IllegalArgumentException(
        "Trip %s not found in timetable for pattern %s on date %s".formatted(
          trip.getId(),
          tripPattern.getId(),
          serviceDate
        )
      );
    }

    int boardingTime = tripTimes.getScheduledDepartureTime(stopPosInPattern);

    return new RoutingOnBoardAccess(
      routeIndex,
      tripScheduleIndex,
      stopPosInPattern,
      raptorStopIndex,
      boardingTime
    );
  }

  /**
   * Resolve the boarding time for an on-board trip location as an {@link Instant}. This uses
   * the transit service to look up the trip, pattern, and scheduled departure time — it does
   * not need the Raptor pattern index, so it can be called early in the pipeline to set the
   * request's dateTime before the search begins.
   */
  public Instant resolveBoardingDateTime(TripLocation tripLocation, ZoneId timeZone) {
    var tripAndServiceDate = resolveTripAndServiceDate(tripLocation.tripOnDateReference());
    var trip = tripAndServiceDate.trip();
    var serviceDate = tripAndServiceDate.serviceDate();

    var tripPattern = transitService.findPattern(trip, serviceDate);
    if (tripPattern == null) {
      tripPattern = transitService.findPattern(trip);
    }
    if (tripPattern == null) {
      throw new IllegalArgumentException(
        "No pattern found for trip %s on date %s".formatted(trip.getId(), serviceDate)
      );
    }

    Integer targetSeconds = toSecondsSinceStartOfService(
      tripLocation.aimedDepartureTime(),
      serviceDate
    );
    int stopPosInPattern = findStopPosition(
      tripPattern,
      tripLocation.stopLocationId(),
      trip,
      serviceDate,
      targetSeconds
    );

    var tripTimes = transitService
      .findTimetable(tripPattern, serviceDate)
      .getTripTimesWithScheduleFallback(trip);
    if (tripTimes == null) {
      throw new IllegalArgumentException(
        "Trip %s not found in timetable for pattern %s on date %s".formatted(
          trip.getId(),
          tripPattern.getId(),
          serviceDate
        )
      );
    }

    int boardingTime = tripTimes.getScheduledDepartureTime(stopPosInPattern);

    var serviceDateStart = ServiceDateUtils.asStartOfService(serviceDate, timeZone);
    return serviceDateStart.plusSeconds(boardingTime).toInstant();
  }

  private TripAndServiceDate resolveTripAndServiceDate(TripOnDateReference reference) {
    if (reference.tripOnServiceDateId() != null) {
      var tripOnServiceDate = transitService.getTripOnServiceDate(reference.tripOnServiceDateId());
      if (tripOnServiceDate == null) {
        throw new IllegalArgumentException(
          "TripOnServiceDate not found: " + reference.tripOnServiceDateId()
        );
      }
      return new TripAndServiceDate(
        tripOnServiceDate.getTrip(),
        tripOnServiceDate.getServiceDate()
      );
    } else if (reference.tripIdOnServiceDate() != null) {
      var tripIdAndDate = reference.tripIdOnServiceDate();
      var trip = transitService.getTrip(tripIdAndDate.tripId());
      if (trip == null) {
        throw new IllegalArgumentException("Trip not found: " + tripIdAndDate.tripId());
      }
      return new TripAndServiceDate(trip, tripIdAndDate.serviceDate());
    }

    throw new IllegalArgumentException(
      "Either tripOnServiceDateId or tripIdOnServiceDate must be set on TripOnDateReference"
    );
  }

  /**
   * Find the trip pattern that exists in the Raptor transit data's pattern index. There are
   * several reasons why the pattern returned by {@code findPattern} may not be directly in the
   * index:
   * <ul>
   *   <li>A realtime update may have created a new pattern not yet in the Raptor data — in that
   *       case the base/static pattern (from {@code findPattern(trip)}) should be used.</li>
   *   <li>During graph build, patterns may be copied, creating a new {@link RoutingTripPattern}
   *       with a different index while the scheduled timetable still references the original.
   *       In that case, the scheduled timetable's pattern should be used.</li>
   * </ul>
   */
  private TripPattern findPatternInRaptorData(
    Trip trip,
    LocalDate serviceDate,
    OnBoardTripPatternSearch patternSearch
  ) {
    var result = findInIndex(transitService.findPattern(trip, serviceDate), patternSearch);
    if (result == null) {
      result = findInIndex(transitService.findPattern(trip), patternSearch);
    }
    if (result == null) {
      throw new IllegalArgumentException(
        "No pattern for trip %s on date %s found in active Raptor data".formatted(
          trip.getId(),
          serviceDate
        )
      );
    }
    return result;
  }

  /**
   * Check if the given pattern (or its scheduledTimetable's pattern) is in the Raptor index.
   * Returns the pattern that is in the index, or null if neither is.
   */
  private static TripPattern findInIndex(
    TripPattern pattern,
    OnBoardTripPatternSearch patternSearch
  ) {
    if (pattern == null) {
      return null;
    }
    if (patternSearch.isInPatternIndex(pattern)) {
      return pattern;
    }
    var scheduledPattern = pattern.getScheduledTimetable().getPattern();
    if (scheduledPattern != pattern && patternSearch.isInPatternIndex(scheduledPattern)) {
      return scheduledPattern;
    }
    return null;
  }

  /**
   * Convert an instant to seconds since start-of-service of the given service date in the
   * transit system's timezone. Returns null if the instant is null.
   * <p>
   * OTP's internal timetable times (e.g. from {@code TripTimes.getScheduledDepartureTime}) are
   * relative to noon-minus-12h (start of service), not midnight. On DST transition days these
   * differ by one hour.
   */
  private Integer toSecondsSinceStartOfService(Instant instant, LocalDate serviceDate) {
    if (instant == null) {
      return null;
    }
    ZoneId timeZone = transitService.getTimeZone();
    long startOfServiceEpochSecond = ServiceDateUtils.asStartOfService(
      serviceDate,
      timeZone
    ).toEpochSecond();
    return (int) (instant.getEpochSecond() - startOfServiceEpochSecond);
  }

  /**
   * Find the stop position in the pattern. The {@code stopLocationId} can refer to either a
   * regular stop (quay) or a station (stop place). If the resolved stops appear more than once
   * and no {@code aimedDepartureTime} is provided, a {@link RoutingValidationException} with
   * {@link RoutingErrorCode#ON_BOARD_LOCATION_MISSING_SCHEDULED_DEPARTURE_TIME} is thrown, signaling that
   * a retry with an {@code aimedDepartureTime} is needed. When an {@code aimedDepartureTime} is provided, it is always
   * validated against the timetable — even for unique stops.
   */
  private int findStopPosition(
    TripPattern tripPattern,
    FeedScopedId stopLocationId,
    Trip trip,
    LocalDate serviceDate,
    Integer aimedDepartureTime
  ) {
    int stopPos;
    // When an aimedDepartureTime is provided, always use it
    // That way, we validate that it matches the timetable data
    if (aimedDepartureTime != null) {
      stopPos = findStopPositionByDepartureTime(
        tripPattern,
        stopLocationId,
        trip,
        serviceDate,
        aimedDepartureTime
      );
    } else {
      stopPos = findSingleStopPosition(tripPattern, stopLocationId, trip);
    }

    int lastStopPos = tripPattern.numberOfStops() - 1;
    if (stopPos == lastStopPos) {
      throw new IllegalArgumentException(
        "Cannot board at the last stop %s of trip %s — no further travel is possible".formatted(
          stopLocationId,
          trip.getId()
        )
      );
    }

    return stopPos;
  }

  /**
   * Find the stop position by matching the aimed departure time (in seconds since midnight,
   * including day offset) among all occurrences of the stop in the pattern.
   */
  private int findStopPositionByDepartureTime(
    TripPattern tripPattern,
    FeedScopedId stopLocationId,
    Trip trip,
    LocalDate serviceDate,
    int targetSeconds
  ) {
    var tripTimes = transitService
      .findTimetable(tripPattern, serviceDate)
      .getTripTimesWithScheduleFallback(trip);
    if (tripTimes == null) {
      throw new IllegalArgumentException(
        "Trip %s not found in timetable for pattern %s on date %s".formatted(
          trip.getId(),
          tripPattern.getId(),
          serviceDate
        )
      );
    }

    for (int i = 0; i < tripPattern.numberOfStops(); i++) {
      var stop = tripPattern.getStop(i);
      if (
        !stop.getId().equals(stopLocationId) && !stop.getStationOrStopId().equals(stopLocationId)
      ) {
        continue;
      }
      int depTime = tripTimes.getScheduledDepartureTime(i);
      if (depTime == targetSeconds) {
        return i;
      }
    }

    throw new IllegalArgumentException(
      "No stop %s with the provided departure time found in pattern for trip %s".formatted(
        stopLocationId,
        trip.getId()
      )
    );
  }

  /**
   * Find a single unambiguous stop position by id
   */
  private int findSingleStopPosition(
    TripPattern tripPattern,
    FeedScopedId stopLocationId,
    Trip trip
  ) {
    var stopPositionInPattern = -1;
    for (int i = 0; i < tripPattern.numberOfStops(); i++) {
      var stop = tripPattern.getStop(i);
      if (stop.getId().equals(stopLocationId) || stop.getStationOrStopId().equals(stopLocationId)) {
        if (stopPositionInPattern >= 0) {
          // Multiple occurrences — need scheduled departure time to disambiguate
          throw new RoutingValidationException(
            List.of(
              new RoutingError(
                RoutingErrorCode.ON_BOARD_LOCATION_MISSING_SCHEDULED_DEPARTURE_TIME,
                InputField.FROM_PLACE
              )
            )
          );
        }

        stopPositionInPattern = i;
      }
    }

    if (stopPositionInPattern < 0) {
      throw new IllegalArgumentException(
        "Stop location %s not found in pattern for trip %s".formatted(stopLocationId, trip.getId())
      );
    }

    return stopPositionInPattern;
  }

  private record TripAndServiceDate(Trip trip, LocalDate serviceDate) {}
}
