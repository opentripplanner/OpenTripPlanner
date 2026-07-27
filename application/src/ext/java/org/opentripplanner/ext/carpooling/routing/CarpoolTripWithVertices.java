package org.opentripplanner.ext.carpooling.routing;

import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Pairs a {@link CarpoolTrip} with the permanent street vertices its route points resolve to, one
 * per route point, in route order.
 */
public final class CarpoolTripWithVertices {

  private final CarpoolTrip trip;
  private final List<Vertex> vertices;
  private final List<WgsCoordinate> vertexCoordinates;

  public CarpoolTripWithVertices(CarpoolTrip trip, List<Vertex> vertices) {
    if (vertices.size() != trip.stops().size()) {
      throw new IllegalArgumentException(
        "Number of vertices (%d) does not match number of stops (%d)".formatted(
          vertices.size(),
          trip.stops().size()
        )
      );
    }
    this.trip = trip;
    this.vertices = List.copyOf(vertices);
    this.vertexCoordinates = this.vertices.stream().map(Vertex::toWgsCoordinate).toList();
  }

  public CarpoolTrip trip() {
    return trip;
  }

  public List<Vertex> vertices() {
    return vertices;
  }

  /**
   * Where the driver is actually routed from and to, which can differ from
   * {@link CarpoolTrip#routePoints()} when a point did not sit on the drivable network.
   */
  public List<WgsCoordinate> vertexCoordinates() {
    return vertexCoordinates;
  }
}
