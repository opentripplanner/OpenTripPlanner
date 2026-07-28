package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper;
import org.opentripplanner.ext.carpooling.util.StreetVertexUtils;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;

/**
 * Resolves each of a {@link CarpoolTrip}'s route points to a permanent, car-reachable street
 * vertex, producing a {@link CarpoolTripWithVertices}. Each point is linked to a temporary vertex
 * via {@link StreetVertexUtils#createDriverWaypointVertex}, then snapped to a permanent one by
 * {@link CarReachableVertexSnapper#snapToPermanentVertex}. {@link #resolve} returns {@code null} if
 * any point cannot be resolved.
 */
public class CarpoolTripVertexResolver {

  /**
   * Reach of the fallback search that relocates a route point onto the drivable network when it
   * does not already sit on a car-reachable vertex; bounded by search travel time (~400 m). A point
   * beyond this is unresolvable.
   */
  private static final Duration MAX_SNAP_SEARCH = Duration.ofMinutes(5);

  private final VertexCreationService vertexCreationService;
  private final CarReachableVertexSnapper carReachableVertexSnapper;

  /**
   * @throws NullPointerException if any parameter is null
   */
  public CarpoolTripVertexResolver(
    VertexCreationService vertexCreationService,
    CarReachableVertexSnapper carReachableVertexSnapper
  ) {
    this.vertexCreationService = Objects.requireNonNull(
      vertexCreationService,
      "vertexCreationService"
    );
    this.carReachableVertexSnapper = Objects.requireNonNull(
      carReachableVertexSnapper,
      "carReachableVertexSnapper"
    );
  }

  /** Resolves every route point to a permanent vertex, or {@code null} if any cannot be resolved. */
  @Nullable
  public CarpoolTripWithVertices resolve(CarpoolTrip trip) {
    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var streetVertexUtils = new StreetVertexUtils(
        vertexCreationService,
        temporaryVerticesContainer
      );
      var vertices = new ArrayList<Vertex>(trip.routePoints().size());
      for (var routePoint : trip.routePoints()) {
        var vertex = resolveRoutePoint(routePoint, streetVertexUtils);
        if (vertex == null) {
          return null;
        }
        vertices.add(vertex);
      }
      return new CarpoolTripWithVertices(trip, vertices);
    }
  }

  @Nullable
  private Vertex resolveRoutePoint(WgsCoordinate point, StreetVertexUtils streetVertexUtils) {
    var linked = streetVertexUtils.createDriverWaypointVertex(point);
    if (linked == null) {
      return null;
    }
    var snap = carReachableVertexSnapper.snapToPermanentVertex(
      StreetSearchRequest.DEFAULT,
      linked,
      MAX_SNAP_SEARCH
    );
    return snap == null ? null : snap.vertex();
  }
}
