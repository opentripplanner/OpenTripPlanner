package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;

import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;

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
    StreetModelForTest.streetEdge(from, to, StreetTraversalPermission.ALL);
    graph.hasStreets = true;
    graph.index();
    graph.calculateConvexHull();

    var vertexLinker = VertexLinkerTestFactory.of(graph);
    var vertexCreationService = new VertexCreationService(vertexLinker);
    var temporaryVerticesContainer = new TemporaryVerticesContainer();
    streetVertexUtils = new StreetVertexUtils(vertexCreationService, temporaryVerticesContainer);
  }

  @Test
  void passenger_createsBidirectionallyLinkedVertex() {
    var result = streetVertexUtils.createPassengerVertex(OSLO_CENTER);

    assertNotNull(result);

    // Splitters reached by following the passenger vertex's outgoing connectors.
    var outgoingSplitters = new HashSet<Vertex>();
    for (var edge : result.getOutgoing()) {
      outgoingSplitters.add(edge.getToVertex());
    }
    // Splitters that have a connector pointing back to the passenger vertex.
    var incomingSplitters = new HashSet<Vertex>();
    for (var edge : result.getIncoming()) {
      incomingSplitters.add(edge.getFromVertex());
    }

    // BIDIRECTIONAL linking requires that the same splitter is both reachable from the passenger
    // vertex AND has a return connector — either direction missing would be a directional-linking
    // regression. This is the universal contract of createPassengerVertex.
    //
    // The car-permitting check below is fixture-dependent: the splitter inherits the underlying
    // edge's permissions, and this test's road has StreetTraversalPermission.ALL, so the splitter
    // is car-routable in both directions. On a walk-only edge the splitter would be walk-only —
    // also correct behavior per the createPassengerVertex Javadoc, just not what this test
    // exercises.
    boolean foundBidirectionalCarSplitter = outgoingSplitters
      .stream()
      .filter(incomingSplitters::contains)
      .anyMatch(
        v ->
          hasCarPermittingStreetEdge(v.getOutgoing()) && hasCarPermittingStreetEdge(v.getIncoming())
      );
    assertTrue(
      foundBidirectionalCarSplitter,
      "Passenger must be linked BIDIRECTIONALLY to a car-accessible splitter so carpool CAR " +
        "routing works in both directions"
    );
  }

  @Test
  void passenger_returnsNullWhenLinkingFails() {
    var farAway = new WgsCoordinate(0.0, 0.0);

    var result = streetVertexUtils.createPassengerVertex(farAway);

    assertNull(result);
  }

  @Test
  void driverWaypoint_createsVertexLinkedToGraph() {
    var result = streetVertexUtils.createDriverWaypointVertex(OSLO_CENTER);

    assertNotNull(result);
    assertFalse(result.getOutgoing().isEmpty());
    assertFalse(result.getIncoming().isEmpty());

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

  @Test
  void driverWaypoint_returnsNullWhenLinkingFails() {
    var farAway = new WgsCoordinate(0.0, 0.0);

    var result = streetVertexUtils.createDriverWaypointVertex(farAway);

    assertNull(result);
  }

  private static boolean hasCarPermittingStreetEdge(
    Iterable<? extends org.opentripplanner.street.model.edge.Edge> edges
  ) {
    for (var e : edges) {
      if (e instanceof StreetEdge se && se.getPermission().allows(TraverseMode.CAR)) {
        return true;
      }
    }
    return false;
  }
}
