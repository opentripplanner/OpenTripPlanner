package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.TemporaryEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;

class TraversalScopeTest extends GraphRoutingTest {

  /**
   * Every candidate {@code permanentBoundary} offers must be permanent, because
   * {@code snapToPermanentVertex} accepts one without re-checking permanence — a temporary candidate
   * would be stored in the carpooling repository and die with the request that produced it. Graph:
   * {@code A --(100 m, all modes)-- B}, linked ~30 % along so A is the nearer endpoint.
   */
  @Test
  void permanentBoundary_offersOnlyPermanentVertices_nearestFirst() {
    var v = new IntersectionVertex[2];
    var model = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          v[0] = intersection("A", 60.0000, 10.0000);
          v[1] = intersection("B", 60.0000, 10.0018);
          street(v[0], v[1], 100, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
        }
      }
    );
    var vertexCreationService = new VertexCreationService(
      VertexLinkerTestFactory.of(model.graph())
    );

    try (var container = new TemporaryVerticesContainer()) {
      var linked = new StreetVertexUtils(
        vertexCreationService,
        container
      ).createDriverWaypointVertex(new WgsCoordinate(60.0000, 10.0005));
      assertNotNull(linked);

      var boundary = TraversalScope.withOwnLinkingOf(linked).permanentBoundary(linked);

      assertFalse(boundary.isEmpty(), "A mid-edge linking must border the permanent graph");
      assertTrue(
        boundary.stream().allMatch(TraversalScope::isPermanent),
        "A temporary candidate would outlive its linking once stored"
      );
      assertEquals(v[0], boundary.get(0), "A is nearer the linked coordinate than B");
      assertTrue(boundary.contains(v[1]), "Both endpoints of the split edge border the linking");
    }
  }

  /**
   * A scope admits the temporary edges of its own linking — a search starting on a temporary vertex
   * needs them to reach the permanent graph at all — but not those of a foreign one, whose mode-blind
   * free edges would let a car cross between networks it cannot drive between.
   * {@link TraversalScope#STATIC_GRAPH} admits neither. Graph:
   * <pre>
   *   A --(100 m, all modes)-- B
   *   A =free= foreign hub
   * </pre>
   */
  @Test
  void shouldSkipEdge_admitsOwnLinkingButNotForeign() {
    var v = new IntersectionVertex[2];
    var hub = new TemporaryStreetLocation[1];
    var model = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          v[0] = intersection("A", 60.0000, 10.0000);
          v[1] = intersection("B", 60.0000, 10.0018);
          street(v[0], v[1], 100, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
          // Stands in for another in-flight request's linking.
          hub[0] = streetLocation("foreign-hub", 60.0001, 10.0004);
          link(v[0], hub[0]);
          link(hub[0], v[0]);
        }
      }
    );
    var vertexCreationService = new VertexCreationService(
      VertexLinkerTestFactory.of(model.graph())
    );

    try (var container = new TemporaryVerticesContainer()) {
      var linked = new StreetVertexUtils(
        vertexCreationService,
        container
      ).createDriverWaypointVertex(new WgsCoordinate(60.0000, 10.0005));
      assertNotNull(linked);

      var scope = TraversalScope.withOwnLinkingOf(linked);
      var permanentEdge = firstEdge(v[1].getOutgoing(), e -> !(e instanceof TemporaryEdge));
      var ownTemporaryEdge = firstEdge(linked.getOutgoing(), e -> e instanceof TemporaryEdge);
      var foreignTemporaryEdge = firstEdge(hub[0].getOutgoing(), e -> e instanceof TemporaryEdge);

      // The state is unused by the predicate, which decides on the edge alone.
      assertFalse(scope.shouldSkipEdge(null, permanentEdge), "Permanent edges are in every scope");
      assertFalse(
        scope.shouldSkipEdge(null, ownTemporaryEdge),
        "Without its own linking a temporary vertex could not reach the permanent graph"
      );
      assertTrue(
        scope.shouldSkipEdge(null, foreignTemporaryEdge),
        "A foreign linking's mode-blind edges must never be crossed"
      );
      assertTrue(
        TraversalScope.STATIC_GRAPH.shouldSkipEdge(null, ownTemporaryEdge),
        "The static graph admits no temporary edge, not even an own one"
      );
    }
  }

  private static Edge firstEdge(Iterable<Edge> edges, Predicate<Edge> match) {
    for (Edge edge : edges) {
      if (match.test(edge)) {
        return edge;
      }
    }
    throw new AssertionError("The test graph has no edge matching the required shape");
  }
}
