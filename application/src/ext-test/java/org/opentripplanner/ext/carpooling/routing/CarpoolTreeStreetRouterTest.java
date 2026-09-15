package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.ext.carpooling.util.StreetVertexUtils;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;

class CarpoolTreeStreetRouterTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final Duration SEARCH_LIMIT = Duration.ofMinutes(30);

  private TestOtpModel model;
  private IntersectionVertex vertexA;
  private IntersectionVertex vertexB;
  private IntersectionVertex vertexC;
  private IntersectionVertex vertexD;
  private IntersectionVertex vertexDisconnected;
  private IntersectionVertex vertexBranch;
  private IntersectionVertex vertexBranchEnd;

  private CarpoolTreeStreetRouter router;

  @BeforeEach
  void setUp() {
    model = modelOf(
      new Builder() {
        @Override
        public void build() {
          var A = intersection("A", ORIGIN);
          var B = intersection("B", ORIGIN.moveEastMeters(500));
          var C = intersection("C", ORIGIN.moveEastMeters(1000));
          var D = intersection("D", ORIGIN.moveEastMeters(1500));
          var Z = intersection("Z", ORIGIN.moveNorthMeters(500));
          // A side branch north of B: far from the A-C line, so it lies outside the ellipse of a
          // leg A -> C but inside the disc of the same duration limit.
          var N = intersection("N", ORIGIN.moveEastMeters(500).moveNorthMeters(1500));
          var M = intersection("M", ORIGIN.moveEastMeters(500).moveNorthMeters(1600));

          biStreet(A, B, 500);
          biStreet(B, C, 500);
          biStreet(C, D, 500);
          biStreet(B, N, 1500);
          biStreet(N, M, 100);
          // Z has no edges — disconnected from the rest of the graph

          vertexA = A;
          vertexB = B;
          vertexC = C;
          vertexD = D;
          vertexDisconnected = Z;
          vertexBranch = N;
          vertexBranchEnd = M;
        }
      }
    );

    router = new CarpoolTreeStreetRouter();
  }

  /**
   * Clears the interrupt flag so that a cancellation raised by one test cannot surface as a
   * spurious {@link OTPRequestTimeoutException} in an unrelated test sharing the same thread.
   */
  @AfterEach
  void clearInterruptFlag() {
    Thread.interrupted();
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

  /**
   * The tree is built inside {@code route}, so a cancelled request surfaces there. A cancellation
   * carries no verdict on whether the leg is routable: it must be raised as an exception rather
   * than reported as a missing path, and it must leave nothing behind that would answer a later
   * query for the same pair. A missing path is memoized — the pair is put in the path cache as
   * {@code null} and every later query for it is answered from the cache without rebuilding the
   * tree — so a cancelled call that entered that cache would make the pair permanently unroutable.
   * The tree registration must survive for the same reason: consuming it leaves no tree to route
   * the pair with.
   */
  @Test
  void propagateCancellationInsteadOfReturningNull() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);

    Thread.currentThread().interrupt();
    assertThrows(OTPRequestTimeoutException.class, () -> router.route(vertexA, vertexC));
    Thread.interrupted();

    assertNotNull(
      router.route(vertexA, vertexC),
      "A cancelled call must leave behind neither a cached null nor a consumed registration"
    );
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

    var segment = router.route(vertexA, vertexC);

    assertNotNull(segment);
    var path = segment.path();
    assertNotNull(path.states, "Path should have states");
    assertFalse(path.states.isEmpty(), "Path states should not be empty");
    assertNotNull(path.edges, "Path should have edges");
    assertFalse(path.edges.isEmpty(), "Path edges should not be empty");
  }

  /**
   * Insertion evaluation only reads durations. The path - a linked list of states and edges - is
   * assembled from the tree's back pointers on demand, so the thousands of segments evaluated and
   * discarded per request never pay for it.
   */
  @Test
  void routeDoesNotBuildThePathUntilAsked() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);

    var segment = (CarpoolTreeStreetRouter.TreeSegment) router.route(vertexA, vertexD);

    assertNotNull(segment);
    assertTrue(segment.durationSeconds() > 0);
    assertFalse(segment.isPathMaterialized(), "Reading the duration must not build the path");

    var path = segment.path();
    assertTrue(segment.isPathMaterialized());
    assertSame(path, segment.path(), "The path is memoised");
  }

  @Test
  void durationIsTheMaterializedPathDurationForBothTreeDirections() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);
    router.addVertex(vertexD, CarpoolTreeStreetRouter.Direction.TO, SEARCH_LIMIT);

    var forward = router.route(vertexA, vertexC);
    var reverse = router.route(vertexB, vertexD);

    assertNotNull(forward);
    assertNotNull(reverse);
    assertEquals(forward.path().getDuration(), forward.durationSeconds());
    assertEquals(reverse.path().getDuration(), reverse.durationSeconds());
    assertEquals(vertexA, forward.from());
    assertEquals(vertexC, forward.to());
    assertEquals(vertexB, reverse.path().states.getFirst().getVertex());
    assertEquals(vertexD, reverse.path().states.getLast().getVertex());
  }

  /**
   * A leg's trees only need the ellipse in which a detour of the leg can still be feasible. The
   * branch end M is within the duration limit of a plain disc around A, but a detour A -> M -> C
   * would take far longer than the limit, so the leg registration must not explore it. The branch
   * vertex N itself is still reached: the check only stops the search from continuing past it.
   */
  @Test
  void addLegBoundsTheForwardTreeToTheLegsEllipse() {
    var disc = new CarpoolTreeStreetRouter();
    disc.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, Duration.ofHours(1));
    var toBranchEnd = disc.route(vertexA, vertexBranchEnd);
    assertNotNull(toBranchEnd);
    var legLimit = toBranchEnd.duration().plusSeconds(5);

    var discWithLegLimit = new CarpoolTreeStreetRouter();
    discWithLegLimit.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, legLimit);
    assertNotNull(
      discWithLegLimit.route(vertexA, vertexBranchEnd),
      "The plain disc of the same limit reaches the branch end"
    );

    router.addLeg(vertexA, vertexC, legLimit);
    assertNotNull(router.route(vertexA, vertexC), "The leg itself is routable");
    assertNotNull(router.route(vertexA, vertexBranch), "The branch vertex is inside the ellipse");
    assertNull(
      router.route(vertexA, vertexBranchEnd),
      "Beyond the branch vertex no detour can return to C within the limit"
    );
    assertEquals(1, router.forwardTreeCount());
    assertEquals(1, router.reverseTreeCount());
  }

  @Test
  void addLegBoundsTheReverseTreeToTheLegsEllipse() {
    var disc = new CarpoolTreeStreetRouter();
    disc.addVertex(vertexC, CarpoolTreeStreetRouter.Direction.TO, Duration.ofHours(1));
    var fromBranchEnd = disc.route(vertexBranchEnd, vertexC);
    assertNotNull(fromBranchEnd);
    var legLimit = fromBranchEnd.duration().plusSeconds(5);

    router.addLeg(vertexA, vertexC, legLimit);
    // Only C has a reverse tree; A's tree is a forward tree, so these queries use the reverse tree.
    assertNotNull(router.route(vertexB, vertexC));
    assertNotNull(router.route(vertexBranch, vertexC));
    assertNull(router.route(vertexBranchEnd, vertexC));
  }

  /**
   * A disc registration at the same vertex means everything within the limit is wanted, so it
   * must widen an ellipse-bounded registration back to a disc.
   */
  @Test
  void addVertexAtALegEndWidensTheTreeBackToADisc() {
    var disc = new CarpoolTreeStreetRouter();
    disc.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, Duration.ofHours(1));
    var legLimit = disc.route(vertexA, vertexBranchEnd).duration().plusSeconds(5);

    router.addLeg(vertexA, vertexC, legLimit);
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, legLimit);

    assertNotNull(router.route(vertexA, vertexBranchEnd));
    assertEquals(1, router.forwardTreeCount(), "Both registrations share one tree");
  }

  /** Two legs from the same start unite their ellipses: what either leg needs stays reachable. */
  @Test
  void legsSharingAStartUniteTheirEllipses() {
    var disc = new CarpoolTreeStreetRouter();
    disc.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, Duration.ofHours(1));
    var legLimit = disc.route(vertexA, vertexBranchEnd).duration().plusSeconds(5);

    router.addLeg(vertexA, vertexC, legLimit);
    router.addLeg(vertexA, vertexBranchEnd, legLimit);

    assertNotNull(router.route(vertexA, vertexC));
    assertNotNull(router.route(vertexA, vertexBranchEnd));
    assertEquals(1, router.forwardTreeCount());
  }

  @Test
  void addLegAfterRoutingStartedThrows() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);
    router.route(vertexA, vertexB);

    assertThrows(IllegalStateException.class, () -> router.addLeg(vertexB, vertexC, SEARCH_LIMIT));
  }

  @Test
  void coLocatedVerticesKeepTheLargestLimit() {
    // Two driver-waypoint vertices at the same coordinate are distinct objects but compare equal
    // (TemporaryStreetLocation equality is coordinate-based), so they share one registration.
    // Registering the small limit last must not shrink the tree below the large one: D (1500 m,
    // ~2 min by car) is reachable only under the 5-minute limit, not the 20-second one.
    var shortLimit = Duration.ofSeconds(20);
    var longLimit = Duration.ofMinutes(5);

    var first = driverWaypointAt(ORIGIN);
    var second = driverWaypointAt(ORIGIN);

    router.addVertex(first, CarpoolTreeStreetRouter.Direction.FROM, longLimit);
    router.addVertex(second, CarpoolTreeStreetRouter.Direction.FROM, shortLimit);

    assertEquals(1, router.forwardTreeCount(), "Co-located vertices should share one tree");
    assertNotNull(
      router.route(first, vertexD),
      "The shared tree must keep the larger limit and still reach the far vertex"
    );
  }

  @Test
  void coLocatedVerticesKeepTheLargestLimitRegardlessOfOrder() {
    var shortLimit = Duration.ofSeconds(20);
    var longLimit = Duration.ofMinutes(5);

    var first = driverWaypointAt(ORIGIN);
    var second = driverWaypointAt(ORIGIN);

    // Small limit first, large limit second — the large limit must still win.
    router.addVertex(first, CarpoolTreeStreetRouter.Direction.FROM, shortLimit);
    router.addVertex(second, CarpoolTreeStreetRouter.Direction.FROM, longLimit);

    assertNotNull(
      router.route(first, vertexD),
      "Registration order must not change the kept limit"
    );
  }

  private Vertex driverWaypointAt(WgsCoordinate coord) {
    var vertexCreationService = new VertexCreationService(
      VertexLinkerTestFactory.of(model.graph())
    );
    var streetVertexUtils = new StreetVertexUtils(
      vertexCreationService,
      new TemporaryVerticesContainer()
    );
    var vertex = streetVertexUtils.createDriverWaypointVertex(coord);
    assertNotNull(vertex, "Driver waypoint vertex should link to the graph");
    return vertex;
  }

  @Test
  void routeFromAToDContainsExpectedEdges() {
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, SEARCH_LIMIT);

    var segment = router.route(vertexA, vertexD);

    assertNotNull(segment, "Should find path from A to D");
    var path = segment.path();
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
