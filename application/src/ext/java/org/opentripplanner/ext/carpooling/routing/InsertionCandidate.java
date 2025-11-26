package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Represents a viable insertion of a passenger into a carpool trip.
 * <p>
 * Contains all information needed to construct an itinerary, including:
 * - The original trip
 * - Insertion positions (where pickup and dropoff occur in the route)
 * - Route segments (all GraphPaths forming the complete modified route)
 * - Timing information (baseline and total duration, deviation)
 */
public record InsertionCandidate(
  CarpoolTrip trip,
  int pickupPosition,
  int dropoffPosition,
  List<GraphPath<State, Edge, Vertex>> routeSegments,
  Duration baselineDuration,
  Duration totalDuration
) {
  /**
   * Calculates the additional duration caused by inserting this passenger.
   */
  public Duration additionalDuration() {
    return totalDuration.minus(baselineDuration);
  }

  /**
   * Checks if this insertion is within the trip's deviation budget.
   */
  public boolean isWithinDeviationBudget() {
    return additionalDuration().compareTo(trip.deviationBudget()) <= 0;
  }

  /**
   * Gets the pickup route segment(s) - from boarding to passenger pickup.
   * Returns all segments before the pickup position.
   */
  public List<GraphPath<State, Edge, Vertex>> getPickupSegments() {
    if (pickupPosition == 0) {
      return List.of();
    }
    return routeSegments.subList(0, pickupPosition);
  }

  /**
   * Gets the shared route segment(s) - from passenger pickup to dropoff.
   * Returns all segments between pickup and dropoff positions.
   */
  public List<GraphPath<State, Edge, Vertex>> getSharedSegments() {
    return routeSegments.subList(pickupPosition, dropoffPosition);
  }

  /**
   * Gets the dropoff route segment(s) - from passenger dropoff to alighting.
   * Returns all segments after the dropoff position.
   */
  public List<GraphPath<State, Edge, Vertex>> getDropoffSegments() {
    if (dropoffPosition >= routeSegments.size()) {
      return List.of();
    }
    return routeSegments.subList(dropoffPosition, routeSegments.size());
  }

  @Override
  public String toString() {
    return String.format(
      "InsertionCandidate{trip=%s, pickup@%d, dropoff@%d, additional=%ds, segments=%d}",
      trip.getId(),
      pickupPosition,
      dropoffPosition,
      additionalDuration().getSeconds(),
      routeSegments.size()
    );
  }
}
