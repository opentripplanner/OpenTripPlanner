package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Represents a viable insertion of a passenger into a carpool trip.
 * <p>
 * Contains all information needed to construct an itinerary, including:
 * <pre>
 * - The original trip
 * - Insertion positions (where pickup and dropoff occur in the modified route)
 * - Route segments (all {@link RoutedSegment}s forming the complete modified route)
 * - Timing information
 * </pre>
 * <p>
 * {@code pickupPosition} and {@code dropoffPosition} are 0-based indices of the passenger's
 * pickup and dropoff stops in the modified route (the route after the passenger's stops have been
 * inserted into the carpool trip).
 */
public record InsertionCandidate(
  CarpoolTrip trip,
  int pickupPosition,
  int dropoffPosition,
  List<RoutedSegment> routeSegments,
  Duration stopDuration,
  @Nullable StopLocation transitStop,
  Duration totalTripDuration,
  @Nullable StreetPath walkToPickup,
  @Nullable StreetPath walkFromDropoff
) {
  /**
   * {@link InsertionEvaluator} guarantees {@code 1 <= pickupPosition < dropoffPosition} (pickup is
   * never at the driver's origin, and dropoff is always strictly after pickup).
   * {@link #getPassengerRideDuration()} relies on the lower bound — it unconditionally adds a
   * boarding dwell, which only makes sense when the passenger boards mid-trip rather than at the
   * trip's start. Enforce both invariants here so a regression upstream fails loud at construction
   * instead of silently producing inconsistent durations.
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

  /**
   * Convenience constructor that derives {@code totalTripDuration} from the route segments and stop
   * duration.
   */
  public InsertionCandidate(
    CarpoolTrip trip,
    int pickupPosition,
    int dropoffPosition,
    List<RoutedSegment> routeSegments,
    Duration stopDuration,
    @Nullable StopLocation transitStop,
    @Nullable StreetPath walkToPickup,
    @Nullable StreetPath walkFromDropoff
  ) {
    this(
      trip,
      pickupPosition,
      dropoffPosition,
      routeSegments,
      stopDuration,
      transitStop,
      totalSegmentDuration(routeSegments, stopDuration),
      walkToPickup,
      walkFromDropoff
    );
  }

  /**
   * Gets the pickup route segment(s) - from boarding to passenger pickup.
   *
   * @return all segments before the pickup position.
   */
  public List<RoutedSegment> getSegmentsBeforePickup() {
    if (pickupPosition == 0) {
      return List.of();
    }
    return routeSegments.subList(0, pickupPosition);
  }

  /**
   * The segments the new passenger rides, from pickup to dropoff.
   *
   * @return all segments between pickup and dropoff positions.
   */
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

  /**
   * Gets the dropoff route segment(s) - from passenger dropoff to alighting.
   *
   * @return all segments after the dropoff position.
   */
  public List<RoutedSegment> getSegmentsAfterDropoff() {
    if (dropoffPosition >= routeSegments.size()) {
      return List.of();
    }
    return routeSegments.subList(dropoffPosition, routeSegments.size());
  }

  /**
   * Calculates the duration from trip start until the car arrives at the passenger's pickup.
   * Includes travel time through pickup segments and intermediate stop delays between them, but
   * <em>excludes</em> the boarding dwell at the pickup itself — that is accounted for in
   * {@link #getPassengerRideDuration()}.
   *
   * @return {@link Duration#ZERO} when the passenger boards at the trip origin (no pickup
   *         segments).
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

  /**
   * Generalized cost of the passenger's ride in raw weight units (seconds-equivalent), equal to
   * {@link #getPassengerRideDuration()} multiplied by {@code carpoolReluctance}.
   */
  public double getPassengerRideWeight(double carpoolReluctance) {
    return getPassengerRideDuration().getSeconds() * carpoolReluctance;
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
      totalTripDuration.getSeconds(),
      routeSegments.size()
    );
  }
}
