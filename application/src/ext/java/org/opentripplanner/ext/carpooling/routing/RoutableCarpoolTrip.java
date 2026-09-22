package org.opentripplanner.ext.carpooling.routing;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Pairs a {@link CarpoolTrip} with the permanent street vertices its route points resolve to, one
 * per route point, in route order, and with its {@link CarpoolCorridor}: what the trip can do for
 * passengers, derived from the vertices and the static street graph when the trip arrives. A trip
 * whose baseline cannot be routed has no corridor and is not routable.
 */
public record RoutableCarpoolTrip(
  CarpoolTrip trip,
  List<Vertex> vertices,
  CarpoolCorridor corridor
) {
  public RoutableCarpoolTrip {
    if (vertices.size() != trip.stops().size()) {
      throw new IllegalArgumentException(
        "Number of vertices (%d) does not match number of stops (%d)".formatted(
          vertices.size(),
          trip.stops().size()
        )
      );
    }
    Objects.requireNonNull(corridor, "corridor");
    if (corridor.legCount() != vertices.size() - 1) {
      throw new IllegalArgumentException(
        "Corridor has %d legs, the trip %d".formatted(corridor.legCount(), vertices.size() - 1)
      );
    }
    vertices = List.copyOf(vertices);
  }
}
