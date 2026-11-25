package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.street.RoutedSegment;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * A viable insertion of a passenger into a carpool trip.
 *
 * @param pickupPosition  the 0-based index of the passenger's pickup among the stops of the route
 *                        with the passenger's two stops inserted
 * @param dropoffPosition the index of the passenger's dropoff in the same route
 * @param routeSegments   the segments of that route, one from each stop to the next
 * @param stopDuration    the dwell at each stop between the first and the last
 * @param transitStop     the transit stop of an access or egress ride
 */
public record InsertionCandidate(
  CarpoolTrip trip,
  int pickupPosition,
  int dropoffPosition,
  List<RoutedSegment> routeSegments,
  Duration stopDuration,
  @Nullable StopLocation transitStop,
  @Nullable StreetPath walkToPickup,
  @Nullable StreetPath walkFromDropoff
) {
  /**
   * The pickup is never at the trip's origin, which {@link #getPassengerRideDuration()} relies on
   * when it adds the boarding dwell, and the dropoff always comes after the pickup.
   */
  public InsertionCandidate {
    if (pickupPosition < 1) {
      throw new IllegalArgumentException(
        "pickupPosition must be >= 1 (pickup at trip origin is invalid); got " + pickupPosition
      );
    }
    if (dropoffPosition <= pickupPosition) {
      throw new IllegalArgumentException(
        "dropoffPosition (" +
          dropoffPosition +
          ") must be > pickupPosition (" +
          pickupPosition +
          ")"
      );
    }
  }

  /** The segments the car drives before it reaches the passenger's pickup. */
  public List<RoutedSegment> getSegmentsBeforePickup() {
    return routeSegments.subList(0, pickupPosition);
  }

  /** The segments the new passenger rides, from the pickup to the dropoff. */
  public List<RoutedSegment> getSegmentsWithNewPassenger() {
    return routeSegments.subList(pickupPosition, dropoffPosition);
  }

  /**
   * The paths of the {@link #getSegmentsWithNewPassenger() segments with the new passenger}, in
   * order.
   */
  public List<StreetPath> getPathsWithNewPassenger() {
    return getSegmentsWithNewPassenger().stream().map(RoutedSegment::path).toList();
  }

  /** The duration of the whole route, the dwells at its intermediate stops included. */
  public Duration totalTripDuration() {
    return totalSegmentDuration(routeSegments, stopDuration);
  }

  /**
   * Calculates the duration from trip start until the car arrives at the passenger's pickup.
   * Includes travel time through pickup segments and intermediate stop delays between them, but
   * <em>excludes</em> the boarding dwell at the pickup itself — that is accounted for in
   * {@link #getPassengerRideDuration()}.
   */
  public Duration getDurationUntilPickupArrival() {
    return totalSegmentDuration(getSegmentsBeforePickup(), stopDuration);
  }

  /**
   * Calculates the duration of the passenger's ride from pickup arrival to dropoff. Includes the
   * boarding dwell at the pickup, travel time through the segments with the new passenger, and stop
   * delays between them.
   */
  public Duration getPassengerRideDuration() {
    return totalSegmentDuration(getSegmentsWithNewPassenger(), stopDuration).plus(stopDuration);
  }

  /** When the car arrives at the passenger's pickup. */
  public ZonedDateTime pickupTime() {
    return trip.startTime().plus(getDurationUntilPickupArrival());
  }

  /** When the car arrives at the passenger's dropoff. */
  public ZonedDateTime dropoffTime() {
    return pickupTime().plus(getPassengerRideDuration());
  }

  /**
   * Generalized cost of the passenger's ride in raw weight units (seconds-equivalent), equal to
   * {@link #getPassengerRideDuration()} multiplied by {@code carpoolReluctance}, plus a fixed
   * {@code boardCost} for getting into the car.
   */
  public double getPassengerRideWeight(double carpoolReluctance, int boardCost) {
    return getPassengerRideDuration().getSeconds() * carpoolReluctance + boardCost;
  }

  private static Duration totalSegmentDuration(
    List<RoutedSegment> segments,
    Duration stopDuration
  ) {
    long segmentSeconds = segments.stream().mapToLong(RoutedSegment::durationSeconds).sum();
    return Duration.ofSeconds(segmentSeconds).plus(
      stopDuration.multipliedBy(Math.max(0, segments.size() - 1))
    );
  }

  @Override
  public String toString() {
    return String.format(
      "InsertionCandidate{trip=%s, pickup@%d, dropoff@%d, duration=%ds, segments=%d}",
      trip.getId(),
      pickupPosition,
      dropoffPosition,
      totalTripDuration().getSeconds(),
      routeSegments.size()
    );
  }
}
