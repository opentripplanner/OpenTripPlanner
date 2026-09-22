package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * What a driver trip can do for passengers, computed once when the trip arrives and reused by
 * every request: the routed duration of each leg, the bound each leg may stretch to, and per leg
 * the transit stops inside its feasibility ellipse with the driving times to serve them.
 * <p>
 * Of the driving times an insertion needs, only those with the passenger at one end are
 * request-specific. The baseline legs and the times between a waypoint and a stop depend on the
 * trip and the static street graph alone, so the corridor holds them and a request evaluates an
 * insertion with a few lookups instead of street searches per trip.
 *
 * @param legDurations routed driving time of each leg
 * @param legLimits the most each leg may take once a passenger is inserted, see
 *        {@link DriverLegLimits}
 * @param stops the stops a driver can serve, with per-leg driving times
 * @param legEnvelopes a bounding box per leg containing the leg's feasibility ellipse
 */
public record CarpoolCorridor(
  List<Duration> legDurations,
  List<Duration> legLimits,
  List<CorridorStop> stops,
  List<Envelope> legEnvelopes
) {
  public CarpoolCorridor {
    legDurations = List.copyOf(legDurations);
    legLimits = List.copyOf(legLimits);
    stops = List.copyOf(stops);
    legEnvelopes = List.copyOf(legEnvelopes);
    if (legLimits.size() != legDurations.size() || legEnvelopes.size() != legDurations.size()) {
      throw new IllegalArgumentException("One limit and one envelope per leg are required");
    }
  }

  /**
   * A transit stop a driver can serve on one leg, with the driving times from the leg's start to
   * the vertex the car stops at and from that vertex to the leg's end. Dropping off and picking
   * up may use different vertices (one-way streets), so both are recorded; a negative value means
   * the stop cannot be served in that role on this leg.
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
    public boolean servesDropoff() {
      return dropoffToStopSeconds >= 0;
    }

    public boolean servesPickup() {
      return pickupToStopSeconds >= 0;
    }
  }

  public int legCount() {
    return legDurations.size();
  }
}
