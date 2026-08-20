package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.util.GraphPathUtils;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.service.StreetLimitationParametersService;

/**
 * Both {@link CarpoolRouter} implementations must report the same duration for the same vertex pair,
 * since {@link InsertionEvaluator} validates an insertion against durations from one and emits
 * geometry from the other. The fixture's two routes differ in speed, length and edge count, so a
 * cost charged per edge or per intersection would make the two dominance functions disagree.
 */
class CarpoolRouterEquivalenceTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);

  /** A 40 m/s ceiling, above every car speed in the fixture, so the heuristic stays admissible. */
  private static final StreetLimitationParametersService STREET_LIMITATION_PARAMETERS =
    StreetLimitationParametersService.DEFAULT;

  /** Far larger than any route through the fixture, so the tree is never the binding constraint. */
  private static final Duration GENEROUS_LIMIT = Duration.ofMinutes(30);

  private IntersectionVertex vertexA;
  private IntersectionVertex vertexZ;

  private CarpoolStreetRouter goalDirectedRouter;
  private CarpoolTreeStreetRouter treeRouter;

  @BeforeEach
  void setUp() {
    modelOf(
      new Builder() {
        @Override
        public void build() {
          var a = intersection("A", ORIGIN);
          var z = intersection("Z", ORIGIN.moveEastMeters(1000));
          // Every declared length exceeds the straight line between its endpoints, keeping the
          // Euclidean heuristic admissible.
          var fast1 = intersection("F1", ORIGIN.moveEastMeters(333).moveNorthMeters(600));
          var fast2 = intersection("F2", ORIGIN.moveEastMeters(667).moveNorthMeters(600));

          // Slow road: one 1000 m edge at 5 m/s = 200 s, drivable both ways.
          street(a, z, 1000, StreetTraversalPermission.ALL, 5f);
          street(z, a, 1000, StreetTraversalPermission.ALL, 5f);

          // Fast road: three edges, 1800 m at 30 m/s = 60 s, drivable towards Z only.
          street(a, fast1, 700, StreetTraversalPermission.ALL, 30f);
          street(fast1, fast2, 400, StreetTraversalPermission.ALL, 30f);
          street(fast2, z, 700, StreetTraversalPermission.ALL, 30f);

          vertexA = a;
          vertexZ = z;
        }
      }
    );

    goalDirectedRouter = new CarpoolStreetRouter(STREET_LIMITATION_PARAMETERS);
    treeRouter = new CarpoolTreeStreetRouter();
  }

  @Test
  void forwardTreeMatchesGoalDirectedRouter() {
    treeRouter.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, GENEROUS_LIMIT);

    assertEquals(
      routedDuration(goalDirectedRouter, vertexA, vertexZ),
      routedDuration(treeRouter, vertexA, vertexZ)
    );
  }

  @Test
  void reverseTreeMatchesGoalDirectedRouter() {
    treeRouter.addVertex(vertexZ, CarpoolTreeStreetRouter.Direction.TO, GENEROUS_LIMIT);

    assertEquals(
      routedDuration(goalDirectedRouter, vertexA, vertexZ),
      routedDuration(treeRouter, vertexA, vertexZ)
    );
  }

  /** The fast road is one-way, so the return leg must fall back on the slow road in both routers. */
  @Test
  void oneWayReturnLegMatchesGoalDirectedRouter() {
    treeRouter.addVertex(vertexZ, CarpoolTreeStreetRouter.Direction.FROM, GENEROUS_LIMIT);

    assertEquals(
      routedDuration(goalDirectedRouter, vertexZ, vertexA),
      routedDuration(treeRouter, vertexZ, vertexA)
    );
  }

  /**
   * The quicker route must also be the cheaper one, or the two dominance functions would rank the
   * pair differently. Its higher edge count is what makes a per-edge cost term visible here.
   */
  @Test
  void theQuickerRouteIsAlsoTheCheaperOne() {
    var outbound = goalDirectedRouter.route(vertexA, vertexZ);
    var returnLeg = goalDirectedRouter.route(vertexZ, vertexA);
    assertNotNull(outbound);
    assertNotNull(returnLeg);

    assertTrue(
      GraphPathUtils.durationOrZero(outbound).compareTo(GraphPathUtils.durationOrZero(returnLeg)) <
        0,
      "the outbound leg takes the fast road and must be quicker than the slow return leg"
    );
    assertTrue(
      outbound.getWeight() < returnLeg.getWeight(),
      "the quicker route must also weigh less, or the two dominance functions would disagree"
    );
    assertTrue(
      outbound.edges.size() > returnLeg.edges.size(),
      "the quicker route must not also be the one with the fewest edges"
    );
  }

  private static Duration routedDuration(CarpoolRouter router, Vertex from, Vertex to) {
    var path = router.route(from, to);
    assertNotNull(path, "no path found from " + from + " to " + to);
    return GraphPathUtils.durationOrZero(path);
  }
}
