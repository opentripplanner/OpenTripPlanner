package org.opentripplanner.ext.carpooling.routing;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.StreetVertexUtils;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Pairs a {@link CarpoolTrip} with its resolved street graph vertices.
 * <p>
 * CarpoolTrip is an immutable domain entity and should not hold mutable routing state.
 * This class provides that association for the duration of a routing request, keeping
 * CarpoolTrip free of graph-level concerns.
 * <p>
 * The vertices list is in the same order as the trip's stops/route points.
 */
public class CarpoolTripWithVertices {

  private final CarpoolTrip trip;
  private final List<Vertex> vertices;

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
  }

  /**
   * Resolves vertices for the trip's route points using the given utilities.
   *
   * @return the trip with vertices, or null if any route point could not be linked to the graph
   */
  @Nullable
  public static CarpoolTripWithVertices create(
    CarpoolTrip trip,
    StreetVertexUtils streetVertexUtils,
    LinkingContext linkingContext
  ) {
    var vertices = trip
      .routePoints()
      .stream()
      .map(coordinate -> streetVertexUtils.getOrCreateVertex(coordinate, linkingContext))
      .toList();
    if (vertices.stream().anyMatch(Objects::isNull)) {
      return null;
    }
    return new CarpoolTripWithVertices(trip, vertices);
  }

  public CarpoolTrip trip() {
    return trip;
  }

  public List<Vertex> vertices() {
    return vertices;
  }
}
