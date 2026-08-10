package org.opentripplanner.ext.carpooling.routing;

import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Pairs a {@link CarpoolTrip} with the permanent street vertices its route points resolve to, one
 * per route point, in route order.
 */
public record CarpoolTripWithVertices(CarpoolTrip trip, List<Vertex> vertices) {
  public CarpoolTripWithVertices {
    if (vertices.size() != trip.stops().size()) {
      throw new IllegalArgumentException(
        "Number of vertices (%d) does not match number of stops (%d)".formatted(
          vertices.size(),
          trip.stops().size()
        )
      );
    }
    vertices = List.copyOf(vertices);
  }
}
