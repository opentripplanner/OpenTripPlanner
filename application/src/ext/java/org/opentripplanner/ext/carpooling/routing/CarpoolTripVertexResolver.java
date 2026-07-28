package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.CarAccessDirection;
import org.opentripplanner.ext.carpooling.util.CarAccessibleVertexSnapper;
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
 * {@link CarAccessibleVertexSnapper#snapToPermanentVertex} for its {@link CarAccessDirection} (see
 * {@link #accessFor}). {@link #resolve} returns {@code null} if any point cannot be resolved.
 */
public class CarpoolTripVertexResolver {

  /**
   * Reach of the fallback search that relocates a route point onto the drivable network when it
   * does not already sit on a car-reachable vertex; bounded by search travel time (~400 m). A point
   * beyond this is unresolvable.
   */
  private static final Duration MAX_SNAP_SEARCH = Duration.ofMinutes(5);

  private final VertexCreationService vertexCreationService;
  private final CarAccessibleVertexSnapper carVertexSnapper;

  /**
   * @throws NullPointerException if any parameter is null
   */
  public CarpoolTripVertexResolver(
    VertexCreationService vertexCreationService,
    CarAccessibleVertexSnapper carVertexSnapper
  ) {
    this.vertexCreationService = Objects.requireNonNull(
      vertexCreationService,
      "vertexCreationService"
    );
    this.carVertexSnapper = Objects.requireNonNull(carVertexSnapper, "carVertexSnapper");
  }

  /** Resolves every route point to a permanent vertex, or {@code null} if any cannot be resolved. */
  @Nullable
  public CarpoolTripWithVertices resolve(CarpoolTrip trip) {
    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var streetVertexUtils = new StreetVertexUtils(
        vertexCreationService,
        temporaryVerticesContainer
      );
      var routePoints = trip.routePoints();
      var vertices = new ArrayList<Vertex>(routePoints.size());
      for (int i = 0; i < routePoints.size(); i++) {
        var vertex = resolveRoutePoint(
          routePoints.get(i),
          accessFor(i, routePoints.size()),
          streetVertexUtils
        );
        if (vertex == null) {
          return null;
        }
        vertices.add(vertex);
      }
      return new CarpoolTripWithVertices(trip, vertices);
    }
  }

  /** First point departs, last arrives, the rest are passed through. */
  private static CarAccessDirection accessFor(int index, int count) {
    if (index == 0) {
      return CarAccessDirection.DEPART;
    }
    if (index == count - 1) {
      return CarAccessDirection.ARRIVE;
    }
    return CarAccessDirection.THROUGH;
  }

  @Nullable
  private Vertex resolveRoutePoint(
    WgsCoordinate point,
    CarAccessDirection access,
    StreetVertexUtils streetVertexUtils
  ) {
    var linked = streetVertexUtils.createDriverWaypointVertex(point);
    if (linked == null) {
      return null;
    }
    var snap = carVertexSnapper.snapToPermanentVertex(
      StreetSearchRequest.DEFAULT,
      linked,
      MAX_SNAP_SEARCH,
      access
    );
    return snap == null ? null : snap.vertex();
  }
}
