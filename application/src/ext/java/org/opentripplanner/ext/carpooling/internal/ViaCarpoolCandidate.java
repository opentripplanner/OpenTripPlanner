package org.opentripplanner.ext.carpooling.internal;

import java.time.Duration;
import java.util.Set;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Represents a viable carpool match with via-route timing information
 */
public record ViaCarpoolCandidate(
  CarpoolTrip trip,
  GraphPath<State, Edge, Vertex> pickupRoute, // A→C (driver to passenger pickup)
  GraphPath<State, Edge, Vertex> sharedRoute, // C→D (shared journey)
  Duration baselineDuration,
  Duration viaDuration,
  Set<Vertex> passengerOrigin,
  Set<Vertex> passengerDestination
) {
  public Duration viaDeviation() {
    return viaDuration.minus(baselineDuration);
  }
}
