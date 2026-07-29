package org.opentripplanner.graph_builder.module.osm;

import java.util.Collection;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/// Efficient lookup class of the spatial index of [OsmVertex]es in a graph that are candidate
/// platform-linking points:
/// single-entry, non-motorized street stubs that a nearby platform's visibility graph may want to
/// link into (for example a stairway landing under a platform).
///
/// The candidate test ([#isPlatformEntranceCandidate]) is a pure edge-topology check that knows
/// nothing about area polygons, so the index can be built once, up front, over every [OsmVertex]
/// in the graph, before any platform's visibility graph exists.
class PlatformEntranceFinder {

  private final SpatialIndex index;

  private PlatformEntranceFinder(SpatialIndex index) {
    this.index = index;
  }

  /// Build the index of platform entrance candidates among `vertices`.
  static PlatformEntranceFinder of(Collection<Vertex> vertices) {
    var index = new STRtree();
    vertices
      .stream()
      .filter(OsmVertex.class::isInstance)
      .map(OsmVertex.class::cast)
      .filter(PlatformEntranceFinder::isPlatformEntranceCandidate)
      .forEach(v -> index.insert(new Envelope(v.getCoordinate()), v));
    // make index immutable
    index.build();
    return new PlatformEntranceFinder(index);
  }

  static PlatformEntranceFinder empty() {
    return new PlatformEntranceFinder(new STRtree());
  }

  /// Return the platform entrance vertices that lie within `polygon`.
  List<OsmVertex> findPlatformVerticesWithin(Polygon polygon) {
    GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
    return query(polygon.getEnvelopeInternal())
      .stream()
      .filter(v -> polygon.contains(geometryFactory.createPoint(v.getCoordinate())))
      .toList();
  }

  @SuppressWarnings("unchecked")
  private List<OsmVertex> query(Envelope envelope) {
    return index.query(envelope);
  }

  /// Tests whether `osmVertex` is a candidate single-entry stub into the street network:
  /// exactly one non-motorized edge (see
  /// [#allowsOnlyNonMotorizedModes]) connects it to one
  /// other vertex, and every other non-[AreaEdge] edge at this vertex leads back to that same
  /// vertex.
  ///
  /// @return `true` if the vertex is a single-entry, non-motorized street stub
  private static boolean isPlatformEntranceCandidate(OsmVertex osmVertex) {
    boolean isCandidate = false;
    Vertex start = null;
    for (Edge e : osmVertex.getIncoming()) {
      if (e instanceof StreetEdge se && !(e instanceof AreaEdge)) {
        if (se.getPermission().allowsOnlyNonMotorizedModes()) {
          isCandidate = true;
          start = se.getFromVertex();
          break;
        }
      }
    }

    if (isCandidate && start != null) {
      boolean isLinkingPoint = true;
      for (Edge e : osmVertex.getOutgoing()) {
        if (
          !e.getToVertex().getCoordinate().equals(start.getCoordinate()) && !(e instanceof AreaEdge)
        ) {
          isLinkingPoint = false;
        }
      }
      return isLinkingPoint;
    }
    return false;
  }
}
