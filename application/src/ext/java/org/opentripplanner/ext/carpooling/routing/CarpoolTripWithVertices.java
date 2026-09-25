package org.opentripplanner.ext.carpooling.routing;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Pairs a {@link CarpoolTrip} with the permanent street vertices its route points resolve to, one
 * per route point, in route order, and — once computed — with its {@link CarpoolCorridor}: what
 * the trip can do for passengers, derived from the vertices and the static street graph when the
 * trip arrives. {@code corridor} is {@code null} while it has not been computed, or when the
 * trip's baseline cannot be routed.
 */
public record CarpoolTripWithVertices(
  CarpoolTrip trip,
  List<Vertex> vertices,
  @Nullable CarpoolCorridor corridor
) {
  public CarpoolTripWithVertices {
    if (vertices.size() != trip.stops().size()) {
      throw new IllegalArgumentException(
        "Number of vertices (%d) does not match number of stops (%d)".formatted(
          vertices.size(),
          trip.stops().size()
        )
      );
    }
    if (corridor != null && corridor.legCount() != vertices.size() - 1) {
      throw new IllegalArgumentException(
        "Corridor has %d legs, the trip %d".formatted(corridor.legCount(), vertices.size() - 1)
      );
    }
    vertices = List.copyOf(vertices);
  }

  public CarpoolTripWithVertices(CarpoolTrip trip, List<Vertex> vertices) {
    this(trip, vertices, null);
  }

  public CarpoolTripWithVertices withCorridor(@Nullable CarpoolCorridor corridor) {
    return new CarpoolTripWithVertices(trip, vertices, corridor);
  }
}
