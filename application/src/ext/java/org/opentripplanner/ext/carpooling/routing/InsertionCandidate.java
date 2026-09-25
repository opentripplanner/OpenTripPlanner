package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * A viable insertion of a passenger into a carpool trip: the trip, where the pickup and dropoff
 * sit in the modified route, and the {@link RoutedSegment}s of that route, whose street paths are
 * only built when an itinerary is (see {@link #getSharedPaths()}).
 * <p>
 * {@code pickupPosition} and {@code dropoffPosition} are 0-based indices of the passenger's
 * pickup and dropoff stops in the modified route (the route after the passenger's stops have
 * been inserted into the carpool trip).
 */
public record InsertionCandidate(
  CarpoolTrip trip,
  int pickupPosition,
  int dropoffPosition,
  List<RoutedSegment> routeSegments,
  Duration stopDuration,
  @Nullable StopLocation transitStop,
  Duration totalTripDuration,
  @Nullable GraphPath<State, Edge, Vertex> walkToPickup,
  @Nullable GraphPath<State, Edge, Vertex> walkFromDropoff
) {
  /**
   * {@code 1 <= pickupPosition < dropoffPosition}: the pickup is never at the driver's origin and
   * the dropoff is strictly after the pickup. {@link #getPassengerRideDuration()} relies on the
   * lower bound, as it always adds a boarding dwell.
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
   * Convenience constructor that derives {@code totalTripDuration} from the route segments and
   * stop duration.
   */
  public InsertionCandidate(
    CarpoolTrip trip,
    int pickupPosition,
    int dropoffPosition,
    List<RoutedSegment> routeSegments,
    Duration stopDuration,
    @Nullable StopLocation transitStop,
    @Nullable GraphPath<State, Edge, Vertex> walkToPickup,
    @Nullable GraphPath<State, Edge, Vertex> walkFromDropoff
  ) {
    this(
      trip,
      pickupPosition,
      dropoffPosition,
      routeSegments,
      stopDuration,
      transitStop,
      computeTotalTripDuration(routeSegments, stopDuration),
      walkToPickup,
      walkFromDropoff
    );
  }

  private static Duration computeTotalTripDuration(
    List<RoutedSegment> routeSegments,
    Duration stopDuration
  ) {
    Duration[] cumulativeDurations = RoutedSegment.cumulativeDurations(routeSegments, stopDuration);
    return cumulativeDurations[cumulativeDurations.length - 1];
  }

  /**
   * Gets the pickup route segment(s) - from boarding to passenger pickup.
   * Returns all segments before the pickup position.
   */
  public List<RoutedSegment> getPickupSegments() {
    if (pickupPosition == 0) {
      return List.of();
    }
    return routeSegments.subList(0, pickupPosition);
  }

  /**
   * Gets the shared route segment(s) - from passenger pickup to dropoff.
   * Returns all segments between pickup and dropoff positions.
   */
  public List<RoutedSegment> getSharedSegments() {
    return routeSegments.subList(pickupPosition, dropoffPosition);
  }

  /**
   * The street paths of the {@link #getSharedSegments() shared segments}, in order. This
   * materialises the paths, which is the expensive part of a segment — call it when building an
   * itinerary from the candidate, not while evaluating candidates.
   */
  public List<GraphPath<State, Edge, Vertex>> getSharedPaths() {
    return getSharedSegments().stream().map(RoutedSegment::path).toList();
  }

  /**
   * Gets the dropoff route segment(s) - from passenger dropoff to alighting.
   * Returns all segments after the dropoff position.
   */
  public List<RoutedSegment> getDropoffSegments() {
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
   * Returns {@link Duration#ZERO} when the passenger boards at the trip origin (no pickup segments).
   */
  public Duration getDurationUntilPickupArrival() {
    return totalSegmentDuration(getPickupSegments(), stopDuration);
  }

  /**
   * Calculates the duration of the passenger's ride from pickup arrival to dropoff. Includes the
   * boarding dwell at the pickup, travel time through shared segments, and stop delays between
   * shared segments.
   */
  public Duration getPassengerRideDuration() {
    return totalSegmentDuration(getSharedSegments(), stopDuration).plus(stopDuration);
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
