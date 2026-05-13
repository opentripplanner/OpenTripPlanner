package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class CarAccessibleVertexSnapperTest extends GraphRoutingTest {

  private IntersectionVertex A;
  private IntersectionVertex B;
  private IntersectionVertex C;
  private IntersectionVertex D;

  /**
   * Graph layout:
   * <pre>
   *   A --(pedestrian)-- B --(pedestrian)-- C --(all modes)-- D
   * </pre>
   * C and D are car-accessible (C thanks to the CD edge; D also via CD). A and B are on
   * pedestrian-only edges and cannot be reached by a driver.
   */
  @BeforeEach
  void setUp() {
    modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          A = intersection("A", 59.9139, 10.7522);
          B = intersection("B", 59.9139, 10.7530);
          C = intersection("C", 59.9139, 10.7540);
          D = intersection("D", 59.9139, 10.7548);

          street(
            A,
            B,
            50,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          street(
            B,
            C,
            70,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          street(C, D, 50, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
        }
      }
    );
  }

  @Test
  void alreadyCarAccessibleVertex_returnsSameVertexWithoutWalk() {
    var result = CarAccessibleVertexSnapper.snapPickup(
      StreetSearchRequest.DEFAULT,
      D,
      Duration.ofMinutes(10)
    );
    assertNotNull(result);
    assertEquals(D, result.vertex());
    assertNull(result.walkPath());
  }

  @Test
  void pedestrianOnlyVertex_withinBudget_snapsToNearestCarAccessible() {
    var result = CarAccessibleVertexSnapper.snapPickup(
      StreetSearchRequest.DEFAULT,
      A,
      Duration.ofMinutes(10)
    );
    assertNotNull(result);
    assertEquals(C, result.vertex());
    assertNotNull(result.walkPath());
    assertTrue(result.walkPath().getDuration() > 0);
  }

  @Test
  void pedestrianOnlyVertex_budgetTooTight_returnsNull() {
    var result = CarAccessibleVertexSnapper.snapPickup(
      StreetSearchRequest.DEFAULT,
      A,
      Duration.ofSeconds(5)
    );
    assertNull(result);
  }

  @Test
  void arriveBySearch_walksBackwardsAlongIncomingEdges() {
    var result = CarAccessibleVertexSnapper.snapDropoff(
      StreetSearchRequest.DEFAULT,
      A,
      Duration.ofMinutes(10)
    );
    assertNotNull(result);
    assertEquals(C, result.vertex());
    assertNotNull(result.walkPath());
    // Walk path must be chronological: starts at C (the car-accessible vertex) and ends at A.
    assertEquals(C, result.walkPath().states.getFirst().getVertex());
    assertEquals(A, result.walkPath().states.getLast().getVertex());
    assertTrue(result.walkPath().getDuration() > 0);
  }

  /**
   * A vertex with car edges in only one direction is not car-accessible: the carpool driver picks
   * up mid-route and must both arrive at and leave the pickup, so a pedestrian-zone vertex hanging
   * off a one-way drivable exit (CAR outgoing only, no incoming) and the corresponding terminus
   * (CAR incoming only, no outgoing) both fail the predicate. The snapper must skip them and walk
   * on to a vertex with car edges in both directions.
   * <p>
   * Graph: {@code S --(ped)-- V --(one-way CAR forward)--> W --(ped only)-- X --(two-way CAR)-- Y}.
   * V has outgoing CAR but no incoming; W has incoming CAR but no outgoing; X is the first
   * fully car-accessible vertex (it has the X↔Y bidirectional drivable pair).
   */
  @Test
  void halfCarAccessibleVertexIsRejected() {
    var holder = new IntersectionVertex[5];
    modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          holder[0] = intersection("S", 60.0000, 10.0000);
          holder[1] = intersection("V", 60.0000, 10.0010);
          holder[2] = intersection("W", 60.0000, 10.0020);
          holder[3] = intersection("X", 60.0000, 10.0030);
          holder[4] = intersection("Y", 60.0000, 10.0040);

          street(
            holder[0],
            holder[1],
            50,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          // V→W: forward allows cars, reverse pedestrian only. V has out-CAR only; W has in-CAR only.
          street(
            holder[1],
            holder[2],
            50,
            StreetTraversalPermission.ALL,
            StreetTraversalPermission.PEDESTRIAN
          );
          // W↔X: pedestrian both ways, so the walker can keep going but neither side gains CAR here.
          street(
            holder[2],
            holder[3],
            50,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          // X↔Y: bidirectional ALL — X gets in-CAR (from Y) and out-CAR (to Y), so X is car-accessible.
          street(
            holder[3],
            holder[4],
            50,
            StreetTraversalPermission.ALL,
            StreetTraversalPermission.ALL
          );
        }
      }
    );

    var result = CarAccessibleVertexSnapper.snapPickup(
      StreetSearchRequest.DEFAULT,
      holder[0],
      Duration.ofMinutes(10)
    );

    assertNotNull(result);
    assertEquals(
      holder[3],
      result.vertex(),
      "Should skip half-car-accessible V and W and snap to X"
    );
  }

  /**
   * The snap must minimize <em>generalized walk weight</em> across reachable car-accessible
   * vertices, not raw distance and not graph-traversal order. Two car-accessible vertices are
   * reachable from S: X is geometrically closer (50m) but reached via an edge with
   * {@code walkSafetyFactor=10}, while Y is twice as far (100m) along a normal-safety edge. By
   * weight Y is cheaper (≈100 weight units vs ≈500), so the snapper must return Y. A buggy
   * implementation that returned the first car-accessible vertex encountered by distance — or
   * that paired the dominance function with a non-trivial heuristic that broke cost-ascending
   * pop order — would land on X.
   * <p>
   * <pre>
   *   S --(walk  50 m, safety x10)--> X &lt;--car--&gt; Xc        weight ~500
   *   S --(walk 100 m, safety  x1)--> Y &lt;--car--&gt; Yc        weight ~100  (cheaper)
   * </pre>
   * Both X and Y are car-accessible: their bidirectional CAR edges to Xc / Yc give them car-in
   * <em>and</em> car-out. The S→X edge is half the distance but its safety factor blows the
   * weight up; S→Y wins despite being farther.
   */
  @Test
  void snapsToMinimumWeightVertex_notNearestByDistance() {
    var holder = new IntersectionVertex[5];
    modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          holder[0] = intersection("S", 60.0000, 10.0000);
          holder[1] = intersection("X", 60.0000, 10.0006);
          holder[2] = intersection("Xc", 60.0000, 10.0012);
          holder[3] = intersection("Y", 60.0000, 9.9988);
          holder[4] = intersection("Yc", 60.0000, 9.9982);

          // S → X: short (50m) but penalized — walkSafetyFactor 10 makes its weight ≈ 500.
          var sx = street(
            holder[0],
            holder[1],
            50,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          sx.forEach(e -> e.setWalkSafetyFactor(10.0f));

          // S → Y: longer (100m) but normal safety — weight ≈ 100, the cheaper snap.
          street(
            holder[0],
            holder[3],
            100,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );

          // X ↔ Xc and Y ↔ Yc give X and Y bidirectional CAR access, making them car-accessible.
          street(
            holder[1],
            holder[2],
            50,
            StreetTraversalPermission.ALL,
            StreetTraversalPermission.ALL
          );
          street(
            holder[3],
            holder[4],
            50,
            StreetTraversalPermission.ALL,
            StreetTraversalPermission.ALL
          );
        }
      }
    );

    var result = CarAccessibleVertexSnapper.snapPickup(
      StreetSearchRequest.DEFAULT,
      holder[0],
      Duration.ofMinutes(10)
    );

    assertNotNull(result);
    assertNotNull(result.walkPath(), "S is pedestrian-only — a real walk path is expected");
    assertEquals(
      holder[3],
      result.vertex(),
      "Should pick Y (low-weight, farther) over X (penalized, closer) by minimum generalized weight"
    );
  }

  /**
   * The walk A* must run with the supplied request's preferences, not the library defaults.
   * Doubling {@code walkReluctance} on otherwise-identical requests should roughly double the
   * walk path's weight
   */
  @Test
  void walkPathWeightUsesSuppliedWalkReluctance() {
    var lowReluctance = StreetSearchRequest.copyOf(StreetSearchRequest.DEFAULT)
      .withWalk(b -> b.withReluctance(1.0))
      .build();
    var highReluctance = StreetSearchRequest.copyOf(StreetSearchRequest.DEFAULT)
      .withWalk(b -> b.withReluctance(4.0))
      .build();

    var lowResult = CarAccessibleVertexSnapper.snapPickup(lowReluctance, A, Duration.ofMinutes(10));
    var highResult = CarAccessibleVertexSnapper.snapPickup(
      highReluctance,
      A,
      Duration.ofMinutes(10)
    );

    assertNotNull(lowResult);
    assertNotNull(highResult);
    assertNotNull(lowResult.walkPath());
    assertNotNull(highResult.walkPath());
    double lowWeight = lowResult.walkPath().getWeight();
    double highWeight = highResult.walkPath().getWeight();
    assertTrue(lowWeight > 1, "Expected non-trivial low-reluctance weight, got " + lowWeight);
    assertTrue(highWeight > 1, "Expected non-trivial high-reluctance weight, got " + highWeight);
    assertTrue(
      highWeight > lowWeight * 2,
      "Weight should scale with walk reluctance: low=" + lowWeight + ", high=" + highWeight
    );
  }
}
