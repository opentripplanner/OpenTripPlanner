package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * What a driver trip can do for passengers, computed once when the trip arrives: the routed
 * duration of each leg, the bound each leg may stretch to, and per leg the transit stops inside its
 * feasibility ellipse with the driving times to serve them.
 * <p>
 * Of the driving times an insertion needs, only those with the passenger at one end are
 * request-specific. The baseline legs and the times between a waypoint and a stop depend on the
 * trip and the static street graph alone, so the corridor holds them.
 *
 * @param legDurations routed driving time of each leg
 * @param legLimits    the most each leg may take once a passenger is inserted: its duration plus
 *                     its deviation budget
 * @param stops        the stops a driver can serve, with per-leg driving times
 * @param waypoints    the permanent street vertices the trip's route points resolve to, one per
 *                     route point, in route order
 * @param maxCarSpeed  the fastest car speed in the street graph, in metres per second
 */
public record CarpoolCorridor(
  List<Duration> legDurations,
  List<Duration> legLimits,
  List<CorridorStop> stops,
  List<Vertex> waypoints,
  double maxCarSpeed
) {
  public CarpoolCorridor {
    legDurations = List.copyOf(legDurations);
    legLimits = List.copyOf(legLimits);
    stops = List.copyOf(stops);
    waypoints = List.copyOf(waypoints);
    if (legLimits.size() != legDurations.size()) {
      throw new IllegalArgumentException("One limit per leg is required");
    }
    if (waypoints.size() != legDurations.size() + 1) {
      throw new IllegalArgumentException(
        "Corridor has %d legs but %d waypoints".formatted(legDurations.size(), waypoints.size())
      );
    }
  }

  /**
   * A transit stop a driver can serve on one leg, with the driving times from the leg's start to
   * the vertex the car stops at and from that vertex to the leg's end. Dropping off and picking up
   * may use different vertices (one-way streets), so both are recorded; a negative value means the
   * stop cannot be served in that role on this leg.
   *
   * @param leg index of the leg, from waypoint {@code leg} to waypoint {@code leg + 1}
   */
  public record CorridorStop(
    FeedScopedId stopId,
    int leg,
    int dropoffToStopSeconds,
    int dropoffFromStopSeconds,
    int pickupToStopSeconds,
    int pickupFromStopSeconds
  ) {
    /** Whether a passenger can be dropped off for this stop on this leg. */
    public boolean servesDropoff() {
      return dropoffToStopSeconds >= 0;
    }

    /** Whether a passenger coming from this stop can be picked up on this leg. */
    public boolean servesPickup() {
      return pickupToStopSeconds >= 0;
    }
  }

  /** The number of legs, one fewer than the trip's stops. */
  public int legCount() {
    return legDurations.size();
  }

  /** The detour bound of the leg from waypoint {@code leg} to the next. */
  DetourBound legBound(int leg) {
    return new DetourBound(
      waypoints.get(leg),
      waypoints.get(leg + 1),
      legLimits.get(leg),
      maxCarSpeed
    );
  }

  /**
   * Whether a passenger at {@code point} can possibly be inserted into some leg: whether a detour
   * through it fits the {@link DetourBound} of a leg. It never rejects a feasible insertion; the
   * exact verdict is the insertion evaluation.
   */
  public boolean mayServe(Vertex point) {
    for (int leg = 0; leg < legLimits.size(); leg++) {
      if (legBound(leg).mayPassThrough(point)) {
        return true;
      }
    }
    return false;
  }
}
