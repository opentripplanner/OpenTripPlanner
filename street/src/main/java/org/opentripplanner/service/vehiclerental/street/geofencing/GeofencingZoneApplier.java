package org.opentripplanner.service.vehiclerental.street.geofencing;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Applies geofencing zone restrictions to the street graph. For restricted zones,
 * {@link GeofencingBoundaryExtension} is applied to boundary-crossing vertices for state-based
 * zone tracking. When {@code applyBusinessAreas} is false, boundary extensions are not created
 * for business-area-only zones, disabling boundary enforcement while keeping the zones in the
 * index for state tracking, speed limits, and debug tiles.
 */
public class GeofencingZoneApplier {

  private final Function<Collection<LineString>, Set<Edge>> findEdgesAlongLineStrings;
  private final Function<Envelope, Collection<Edge>> findEdgesForEnvelope;
  private final boolean applyBusinessAreas;

  public GeofencingZoneApplier(
    Function<Collection<LineString>, Set<Edge>> findEdgesAlongLineStrings,
    Function<Envelope, Collection<Edge>> findEdgesForEnvelope,
    boolean applyBusinessAreas
  ) {
    this.findEdgesAlongLineStrings = findEdgesAlongLineStrings;
    this.findEdgesForEnvelope = findEdgesForEnvelope;
    this.applyBusinessAreas = applyBusinessAreas;
  }

  /**
   * Applies the restrictions described in the geofencing zones to edges, builds a spatial index,
   * and identifies boundary-crossing edges.
   */
  public GeofencingZoneApplierResult applyGeofencingZones(
    Collection<GeofencingZone> geofencingZones
  ) {
    var zoneIndex = new GeofencingZoneIndex(geofencingZones);

    // All zones with geometry get boundary extensions, not just restricted ones.
    // Permissive zones need boundary tracking so they enter/exit state correctly
    // and can contribute values via per-field precedence.
    // When applyBusinessAreas is false, business-area-only zones are excluded from
    // boundary detection so that BusinessAreaEnforcement never triggers, but the
    // zones remain in the index for state tracking, speed limits, and debug tiles.
    var zonesWithGeometry = geofencingZones
      .stream()
      .filter(z -> z.geometry() != null)
      .filter(z -> applyBusinessAreas || !z.isBusinessArea())
      .toList();

    var boundaryVertices = addBoundaryExtensions(zonesWithGeometry);

    return new GeofencingZoneApplierResult(Set.copyOf(boundaryVertices), zoneIndex);
  }

  /**
   * Pre-resolves the initial geofencing zones for each vehicle rental vertex by querying the
   * spatial index. All containing zones are stored on the vertex so that the routing state is
   * correctly initialized when a rental begins.
   */
  public static void preResolveVertexZones(
    Collection<VehicleRentalPlaceVertex> vertices,
    GeofencingZoneIndex zoneIndex
  ) {
    for (var vertex : vertices) {
      var zones = zoneIndex.getZonesContaining(vertex.getCoordinate());
      vertex.setInitialGeofencingZones(Set.copyOf(zones));
    }
  }

  /**
   * Identifies boundary-crossing edges and applies {@link GeofencingBoundaryExtension}. A
   * boundary-crossing edge has one vertex inside the zone and one outside. Uses vertex coordinates
   * to detect crossings without decompressing the compact edge geometry.
   */
  private Set<Vertex> addBoundaryExtensions(List<GeofencingZone> zones) {
    var verticesUpdated = new HashSet<Vertex>();
    var gf = GeometryUtils.getGeometryFactory();
    var reusablePoint = gf.createPoint(new Coordinate(0, 0));

    for (GeofencingZone zone : zones) {
      var geom = zone.geometry();
      var preparedZone = PreparedGeometryFactory.prepare(geom);
      var zoneBBox = geom.getEnvelopeInternal();

      // Query edges near the zone boundary rather than the full bounding box.
      // For zones with large bboxes (e.g. world-spanning polygons from GBFS data),
      // querying the full bbox would return millions of irrelevant interior edges.
      Collection<Edge> candidates = getBoundaryEdgeCandidates(geom.getBoundary());

      // Cache vertex containment per zone. Edges share vertices (typically 3-4 edges per
      // vertex), so caching avoids redundant PreparedGeometry.covers() calls.
      var vertexInZone = new HashMap<Vertex, Boolean>();

      for (var e : candidates) {
        if (e instanceof StreetEdge streetEdge) {
          addBoundaryIfCrossing(
            streetEdge,
            zone,
            zoneBBox,
            preparedZone,
            reusablePoint,
            vertexInZone,
            verticesUpdated
          );
        }
      }
    }
    return verticesUpdated;
  }

  /**
   * Find candidate edges near a zone boundary. Uses per-segment spatial queries along the
   * boundary line strings instead of a single bounding box query, which avoids returning
   * millions of interior edges for zones with large geographic extent.
   */
  private Collection<Edge> getBoundaryEdgeCandidates(Geometry boundary) {
    if (boundary instanceof LineString ring) {
      return findEdgesAlongLineStrings.apply(List.of(ring));
    } else if (boundary instanceof MultiLineString mls) {
      return findEdgesAlongLineStrings.apply(GeometryUtils.getLineStrings(mls));
    } else {
      return findEdgesForEnvelope.apply(boundary.getEnvelopeInternal());
    }
  }

  private static void addBoundaryIfCrossing(
    StreetEdge streetEdge,
    GeofencingZone zone,
    Envelope zoneBBox,
    PreparedGeometry preparedZone,
    Point reusablePoint,
    Map<Vertex, Boolean> vertexInZone,
    Set<Vertex> verticesUpdated
  ) {
    var fromVertex = streetEdge.getFromVertex();
    var toVertex = streetEdge.getToVertex();

    boolean fromInZone = vertexInZone.computeIfAbsent(fromVertex, v ->
      isVertexInZone(v.getCoordinate(), zoneBBox, preparedZone, reusablePoint)
    );
    boolean toInZone = vertexInZone.computeIfAbsent(toVertex, v ->
      isVertexInZone(v.getCoordinate(), zoneBBox, preparedZone, reusablePoint)
    );

    if (fromInZone != toInZone) {
      fromVertex.addGeofencingBoundary(new GeofencingBoundaryExtension(zone, toInZone));
      toVertex.addGeofencingBoundary(new GeofencingBoundaryExtension(zone, !toInZone));
      verticesUpdated.add(fromVertex);
      verticesUpdated.add(toVertex);
    }
  }

  private static boolean isVertexInZone(
    Coordinate coord,
    Envelope zoneBBox,
    PreparedGeometry preparedZone,
    Point reusablePoint
  ) {
    if (!zoneBBox.contains(coord)) {
      return false;
    }
    reusablePoint.getCoordinateSequence().setOrdinate(0, 0, coord.x);
    reusablePoint.getCoordinateSequence().setOrdinate(0, 1, coord.y);
    reusablePoint.geometryChanged();
    return preparedZone.covers(reusablePoint);
  }
}
