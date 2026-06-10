package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

class CarpoolTreeStreetRouterTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final Duration SEARCH_LIMIT = Duration.ofMinutes(30);

  private IntersectionVertex vertexA;
  private IntersectionVertex vertexB;
  private IntersectionVertex vertexC;
  private IntersectionVertex vertexD;
  private IntersectionVertex vertexDisconnected;

  private CarpoolTreeStreetRouter router;

  @BeforeEach
  void setUp() {
    modelOf(
      new Builder() {
        @Override
        public void build() {
          var A = intersection("A", ORIGIN);
          var B = intersection("B", ORIGIN.moveEastMeters(500));
          var C = intersection("C", ORIGIN.moveEastMeters(1000));
          var D = intersection("D", ORIGIN.moveEastMeters(1500));
          var Z = intersection("Z", ORIGIN.moveNorthMeters(500));

          biStreet(A, B, 500);
          biStreet(B, C, 500);
          biStreet(C, D, 500);
          // Z has no edges — disconnected from the rest of the graph

          vertexA = A;
          vertexB = B;
          vertexC = C;
          vertexD = D;
          vertexDisconnected = Z;
        }
      }
    );

    router = new CarpoolTreeStreetRouter();
  }

  @Test
  void routeFromVertexWithForwardTree() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);

    var path = router.route(vertexA, vertexC);

    assertNotNull(path, "Should find path from A to C using forward tree");
  }

  @Test
  void routeToVertexWithReverseTree() {
    router.addVertex(vertexC, CarpoolTreeStreetRouter.Direction.TO, SEARCH_LIMIT);

    var path = router.route(vertexA, vertexC);

    assertNotNull(path, "Should find path from A to C using reverse tree");
  }

  @Test
  void routeWithBothDirectionTree() {
    router.addVertex(vertexB, CarpoolTreeStreetRouter.Direction.BOTH, SEARCH_LIMIT);

    var fromB = router.route(vertexB, vertexC);
    var toB = router.route(vertexA, vertexB);

    assertNotNull(fromB, "Should find path from B using forward tree");
    assertNotNull(toB, "Should find path to B using reverse tree");
  }

  @Test
  void routeReturnsNullWhenNoTreeExists() {
    var path = router.route(vertexA, vertexC);

    assertNull(path, "Should return null when no tree exists for either vertex");
  }

  @Test
  void routeCachesResults() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);

    var first = router.route(vertexA, vertexC);
    var second = router.route(vertexA, vertexC);

    assertNotNull(first);
    assertSame(first, second, "Second call should return cached path");
  }

  @Test
  void routePrefersForwardTreeOverReverseTree() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);
    router.addVertex(vertexC, CarpoolTreeStreetRouter.Direction.TO, SEARCH_LIMIT);

    var path = router.route(vertexA, vertexC);

    assertNotNull(path, "Should find path when both trees are available");
  }

  @Test
  void addVertexFromDoesNotCreateReverseTree() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);

    // A has a forward tree, so routing FROM A works
    assertNotNull(router.route(vertexA, vertexC));

    // But there's no reverse tree for A, and no forward tree for D
    // So routing from D to A should fail (return null)
    assertNull(
      router.route(vertexD, vertexA),
      "Should not find path TO A when only forward tree was created"
    );
  }

  @Test
  void addVertexToDoesNotCreateForwardTree() {
    router.addVertex(vertexC, CarpoolTreeStreetRouter.Direction.TO, SEARCH_LIMIT);

    // C has a reverse tree, so routing TO C works
    assertNotNull(router.route(vertexA, vertexC));

    // But no forward tree for C
    assertNull(
      router.route(vertexC, vertexD),
      "Should not find path FROM C when only reverse tree was created"
    );
  }

  @Test
  void addVertexIsIdempotent() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);

    assertEquals(
      1,
      router.forwardTreeCount(),
      "Adding same vertex twice should not create duplicate trees"
    );
    assertEquals(
      0,
      router.reverseTreeCount(),
      "No reverse tree should be created for FROM direction"
    );

    var path = router.route(vertexA, vertexC);
    assertNotNull(path, "Should still work after adding same vertex twice");
  }

  @Test
  void multipleVerticesCanBeAdded() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);
    router.addVertex(vertexD, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);

    assertNotNull(router.route(vertexA, vertexC));
    assertNotNull(router.route(vertexD, vertexB));
  }

  @Test
  void routeReturnsNullForUnreachableVertex() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);

    var path = router.route(vertexA, vertexDisconnected);
    assertNull(path, "Should return null for unreachable vertex in the same graph");
  }

  @Test
  void shortSearchLimitFindsNearbyButNotFarVertices() {
    // 500m at ~13 m/s (car speed) is ~38 seconds
    var shortLimit = Duration.ofSeconds(40);
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, shortLimit);

    var nearbyPath = router.route(vertexA, vertexB);
    assertNotNull(nearbyPath, "Should find nearby vertex B within short search limit");

    var farPath = router.route(vertexA, vertexD);
    assertNull(farPath, "Should not find far vertex D within short search limit");
  }

  @Test
  void routePathIsNonEmpty() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);

    var path = router.route(vertexA, vertexC);

    assertNotNull(path);
    assertNotNull(path.states, "Path should have states");
    assertFalse(path.states.isEmpty(), "Path states should not be empty");
    assertNotNull(path.edges, "Path should have edges");
    assertFalse(path.edges.isEmpty(), "Path edges should not be empty");
  }

  @Test
  void routeFromAToDContainsExpectedEdges() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);

    var path = router.route(vertexA, vertexD);

    assertNotNull(path, "Should find path from A to D");
    assertEquals(3, path.edges.size(), "Path should have 3 edges (A->B, B->C, C->D)");

    var edgeAB = path.edges.get(0);
    assertEquals(vertexA, edgeAB.getFromVertex(), "First edge should start at A");
    assertEquals(vertexB, edgeAB.getToVertex(), "First edge should end at B");

    var edgeBC = path.edges.get(1);
    assertEquals(vertexB, edgeBC.getFromVertex(), "Second edge should start at B");
    assertEquals(vertexC, edgeBC.getToVertex(), "Second edge should end at C");

    var edgeCD = path.edges.get(2);
    assertEquals(vertexC, edgeCD.getFromVertex(), "Third edge should start at C");
    assertEquals(vertexD, edgeCD.getToVertex(), "Third edge should end at D");
  }
}
