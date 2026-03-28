package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;

class StreetVertexUtilsTest {

  private static final double DELTA = 0.001;

  private StreetVertex from;
  private StreetVertex to;
  private StreetVertexUtils streetVertexUtils;

  @BeforeEach
  void setup() {
    from = StreetModelForTest.intersectionVertex(
      OSLO_CENTER.latitude() - DELTA,
      OSLO_CENTER.longitude() - DELTA
    );
    to = StreetModelForTest.intersectionVertex(
      OSLO_CENTER.latitude() + DELTA,
      OSLO_CENTER.longitude() + DELTA
    );

    var graph = new Graph();
    graph.addVertex(from);
    graph.addVertex(to);
    StreetModelForTest.streetEdge(from, to, StreetTraversalPermission.CAR);
    graph.hasStreets = true;
    graph.index();
    graph.calculateConvexHull();

    var vertexLinker = VertexLinkerTestFactory.of(graph);
    var temporaryVerticesContainer = new TemporaryVerticesContainer();
    streetVertexUtils = new StreetVertexUtils(vertexLinker, temporaryVerticesContainer);
  }

  @Test
  void returnsExistingVertexFromLinkingContext() {
    var location = GenericLocation.fromCoordinate(OSLO_CENTER.latitude(), OSLO_CENTER.longitude());
    var existingVertex = StreetModelForTest.intersectionVertex(
      OSLO_CENTER.latitude(),
      OSLO_CENTER.longitude()
    );
    var linkingContext = new LinkingContext(
      Map.of(location, Set.of(existingVertex)),
      Set.of(),
      Set.of()
    );

    var result = streetVertexUtils.getOrCreateVertex(OSLO_CENTER, linkingContext);

    assertEquals(existingVertex, result);
  }

  @Test
  void createsTemporaryVertexWhenNotInContext() {
    var emptyContext = new LinkingContext(Map.of(), Set.of(), Set.of());

    var result = streetVertexUtils.getOrCreateVertex(OSLO_CENTER, emptyContext);

    assertNotNull(result);
  }

  @Test
  void returnsNullWhenLinkingFails() {
    var emptyContext = new LinkingContext(Map.of(), Set.of(), Set.of());
    // Coordinate far from any graph edge
    var farAway = new WgsCoordinate(0.0, 0.0);

    var result = streetVertexUtils.getOrCreateVertex(farAway, emptyContext);

    assertNull(result);
  }

  @Test
  void createdVertexIsLinkedToGraphVertices() {
    var emptyContext = new LinkingContext(Map.of(), Set.of(), Set.of());

    var result = streetVertexUtils.getOrCreateVertex(OSLO_CENTER, emptyContext);

    assertNotNull(result);
    assertFalse(result.getOutgoing().isEmpty());
    assertFalse(result.getIncoming().isEmpty());

    // The temporary vertex is linked to split points on the street edge between from and to.
    // Walk two hops to verify the split points connect back to the original graph vertices.
    var twoHopOutgoing = new HashSet<Vertex>();
    for (var edge : result.getOutgoing()) {
      for (var nextEdge : edge.getToVertex().getOutgoing()) {
        twoHopOutgoing.add(nextEdge.getToVertex());
      }
    }
    assertTrue(twoHopOutgoing.contains(from) || twoHopOutgoing.contains(to));

    var twoHopIncoming = new HashSet<Vertex>();
    for (var edge : result.getIncoming()) {
      for (var nextEdge : edge.getFromVertex().getIncoming()) {
        twoHopIncoming.add(nextEdge.getFromVertex());
      }
    }
    assertTrue(twoHopIncoming.contains(from) || twoHopIncoming.contains(to));
  }
}
