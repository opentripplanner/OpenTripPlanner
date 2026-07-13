package org.opentripplanner.street.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelFactory.streetEdge;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.TestGeofencingZoneBuilder;
import org.opentripplanner.service.vehiclerental.street.geofencing.GeofencingBoundaryExtension;
import org.opentripplanner.service.vehiclerental.street.geofencing.GeofencingZoneIndex;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetConstants;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.search.TraverseModeSet;

/**
 * Tests that split vertices on boundary-crossing edges get spatially correct
 * geofencing boundary extensions instead of blind-copied ones from parent vertices.
 */
class VertexLinkerGeofencingTest {

  // Zone polygon: a rectangle covering lon [10.70, 10.71], lat [59.92, 59.93]
  private static final Polygon ZONE_POLYGON = GeometryUtils.getGeometryFactory().createPolygon(
    new Coordinate[] {
      new Coordinate(10.70, 59.92),
      new Coordinate(10.71, 59.92),
      new Coordinate(10.71, 59.93),
      new Coordinate(10.70, 59.93),
      new Coordinate(10.70, 59.92),
    }
  );

  private static final GeofencingZone NO_DROP_OFF_ZONE = TestGeofencingZoneBuilder.of(
    "tier",
    "park"
  )
    .withGeometry(ZONE_POLYGON)
    .noDropOff()
    .build();

  // A is outside the zone (lon=10.695), B is inside (lon=10.705)
  // Both at lat=59.925 (midpoint of zone's lat range)
  private final StreetVertex vertexOutside = intersectionVertex("A", 59.925, 10.695);
  private final StreetVertex vertexInside = intersectionVertex("B", 59.925, 10.705);

  @Test
  void splitVertexInsideZoneGetsEnteringFalse() {
    // Split at a point inside the zone (lon=10.703, which is inside [10.70, 10.71])
    var splitVertex = linkAndFindSplitVertex(59.925, 10.703);

    var boundaries = splitVertex.listGeofencingBoundaries();
    assertEquals(1, boundaries.size());
    var boundary = boundaries.getFirst();
    assertEquals(NO_DROP_OFF_ZONE, boundary.zone());
    // Inside the zone → entering=false
    assertFalse(boundary.entering());
  }

  @Test
  void splitVertexOutsideZoneGetsEnteringTrue() {
    // Split at a point outside the zone (lon=10.698, which is outside [10.70, 10.71])
    var splitVertex = linkAndFindSplitVertex(59.925, 10.698);

    var boundaries = splitVertex.listGeofencingBoundaries();
    assertEquals(1, boundaries.size());
    var boundary = boundaries.getFirst();
    assertEquals(NO_DROP_OFF_ZONE, boundary.zone());
    // Outside the zone → entering=true
    assertTrue(boundary.entering());
  }

  /**
   * When a split vertex is created inside a zone on a boundary-crossing edge, the parent
   * edge's fromVertex must NOT get a boundary extension added. Only the split vertex itself
   * should receive a boundary — fromVertex may be an interior vertex with no boundary crossings,
   * and adding one would break zone tracking during traversal.
   */
  @Test
  void splitVertexInsideZoneDoesNotAddBoundaryToParentFromVertex() {
    var splitVertex = linkAndFindSplitVertex(59.925, 10.703);

    // The split vertex itself should have a boundary (entering=false, it's inside)
    assertEquals(1, splitVertex.listGeofencingBoundaries().size());
    assertFalse(splitVertex.listGeofencingBoundaries().getFirst().entering());

    // vertexOutside (fromVertex of the original edge) should only have its own boundary
    assertEquals(1, vertexOutside.listGeofencingBoundaries().size());
    assertTrue(vertexOutside.listGeofencingBoundaries().getFirst().entering());
  }

  @Test
  void splitVertexOnNonBoundaryEdgeGetsNoBoundaries() {
    // Two vertices both outside the zone
    var v1 = intersectionVertex("C", 59.925, 10.690);
    var v2 = intersectionVertex("D", 59.925, 10.695);
    streetEdge(v1, v2);

    var graph = new Graph();
    graph.addVertex(v1);
    graph.addVertex(v2);
    graph.index();

    var linker = new VertexLinker(
      graph,
      zoneService(),
      VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES,
      StreetConstants.DEFAULT_MAX_AREA_NODES,
      true
    );

    var split = new TemporaryStreetLocation(
      new Coordinate(10.6925, 59.925),
      I18NString.of("split")
    );
    var disposable = linker.linkVertexForRequest(
      split,
      TraverseModeSet.allModes(),
      LinkingDirection.BIDIRECTIONAL,
      (sv1, sv2) ->
        List.of(TemporaryFreeEdge.createTemporaryFreeEdge((TemporaryStreetLocation) sv1, sv2))
    );

    // Find the split vertex
    var splitterVertex = findSplitterVertex(split);
    assertTrue(
      splitterVertex.listGeofencingBoundaries().isEmpty(),
      "Split vertex on non-boundary edge should have no geofencing boundaries"
    );

    disposable.disposeEdges();
  }

  private SplitterVertex linkAndFindSplitVertex(double lat, double lon) {
    streetEdge(vertexOutside, vertexInside);

    // Add boundary extensions to simulate what GeofencingZoneApplier does:
    // A (outside) gets entering=true, B (inside) gets entering=false
    vertexOutside.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, true));
    vertexInside.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, false));

    var graph = new Graph();
    graph.addVertex(vertexOutside);
    graph.addVertex(vertexInside);
    graph.index();

    var linker = new VertexLinker(
      graph,
      zoneService(),
      VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES,
      StreetConstants.DEFAULT_MAX_AREA_NODES,
      true
    );

    var split = new TemporaryStreetLocation(new Coordinate(lon, lat), I18NString.of("split"));
    var disposable = linker.linkVertexForRequest(
      split,
      TraverseModeSet.allModes(),
      LinkingDirection.BIDIRECTIONAL,
      (v1, v2) ->
        List.of(TemporaryFreeEdge.createTemporaryFreeEdge((TemporaryStreetLocation) v1, v2))
    );

    var splitterVertex = findSplitterVertex(split);
    disposable.disposeEdges();
    return splitterVertex;
  }

  private static SplitterVertex findSplitterVertex(TemporaryStreetLocation location) {
    for (var edge : location.getOutgoing()) {
      if (edge.getToVertex() instanceof SplitterVertex sv) {
        return sv;
      }
    }
    for (var edge : location.getIncoming()) {
      if (edge.getFromVertex() instanceof SplitterVertex sv) {
        return sv;
      }
    }
    return fail("No SplitterVertex found connected to the temporary location");
  }

  private static GeofencingZoneService zoneService() {
    var index = new GeofencingZoneIndex(Set.of(NO_DROP_OFF_ZONE));
    return new GeofencingZoneService() {
      @Override
      public Set<GeofencingZone> zonesContaining(Coordinate coord) {
        return index.findZonesContaining(coord);
      }

      @Override
      public boolean hasIndexedZones() {
        return true;
      }

      @Override
      public Set<GeofencingZone> listZones() {
        return index.listZones();
      }
    };
  }
}
