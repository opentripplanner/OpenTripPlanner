package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
 * Turns a {@link CarpoolTrip} into a {@link RoutableCarpoolTrip}: each route point is resolved to a
 * permanent, car-reachable street vertex, and the trip's {@link CarpoolCorridor} is computed from
 * the vertices. A point is linked to a temporary vertex via
 * {@link StreetVertexUtils#createDriverWaypointVertex}, then snapped to a permanent one by
 * {@link CarReachableVertexSnapper#snapToPermanentVertex}. {@link #resolve} returns {@code null} if
 * any point cannot be resolved or the trip's baseline cannot be routed.
 */
public class RoutableCarpoolTripResolver {

  private final VertexCreationService vertexCreationService;
  private final CarReachableVertexSnapper carReachableVertexSnapper;
  private final CorridorBuilder corridorBuilder;

  /**
   * Reach of the fallback search that relocates a route point onto the drivable network when it
   * does not already sit on a car-reachable vertex; a point beyond this is unresolvable.
   */
  private final Duration maxRoutePointSnap;

  /**
   * @throws NullPointerException if any parameter is null
   */
  public RoutableCarpoolTripResolver(
    VertexCreationService vertexCreationService,
    CarReachableVertexSnapper carReachableVertexSnapper,
    CorridorBuilder corridorBuilder,
    Duration maxRoutePointSnap
  ) {
    this.vertexCreationService = Objects.requireNonNull(
      vertexCreationService,
      "vertexCreationService"
    );
    this.carReachableVertexSnapper = Objects.requireNonNull(
      carReachableVertexSnapper,
      "carReachableVertexSnapper"
    );
    this.corridorBuilder = Objects.requireNonNull(corridorBuilder, "corridorBuilder");
    this.maxRoutePointSnap = Objects.requireNonNull(maxRoutePointSnap, "maxRoutePointSnap");
  }

  /**
   * Resolves every route point to a permanent vertex and computes the trip's corridor from them;
   * {@code null} if any point cannot be resolved or the baseline cannot be routed.
   */
  @Nullable
  public RoutableCarpoolTrip resolve(CarpoolTrip trip) {
    var vertices = new ArrayList<Vertex>(trip.routePoints().size());
    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var streetVertexUtils = new StreetVertexUtils(
        vertexCreationService,
        temporaryVerticesContainer
      );
      for (var routePoint : trip.routePoints()) {
        var vertex = resolveRoutePoint(routePoint, streetVertexUtils);
        if (vertex == null) {
          return null;
        }
        vertices.add(vertex);
      }
    }
    // The corridor is computed after the temporary linking is gone: the resolved vertices are
    // permanent, and the searches should see the static graph only.
    return resolveOnVertices(trip, vertices);
  }

  /**
   * The trip on already resolved vertices, with its corridor computed for its current stops;
   * {@code null} if the baseline cannot be routed.
   */
  @Nullable
  public RoutableCarpoolTrip resolveOnVertices(CarpoolTrip trip, List<Vertex> vertices) {
    var corridor = corridorBuilder.build(trip, vertices);
    return corridor == null ? null : new RoutableCarpoolTrip(trip, vertices, corridor);
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
      maxRoutePointSnap
    );
    return snap == null ? null : snap.vertex();
  }
}
