package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.edge.TemporaryFreeEdge.createTemporaryFreeEdge;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class CarReachableVertexSnapperTest extends GraphRoutingTest {

  private IntersectionVertex A;
  private IntersectionVertex B;
  private IntersectionVertex C;
  private IntersectionVertex D;

  /**
   * Small escape distance so these compact test graphs (car edges tens of metres) pass the
   * reachability check; the reachability-specific tests build their own snapper.
   */
  private final CarReachableVertexSnapper snapper = new CarReachableVertexSnapper(20);

  /**
   * <pre>
   *   A --(pedestrian)-- B --(pedestrian)-- C --(all modes)-- D
   * </pre>
   * C and D are car-reachable via CD; A and B are pedestrian-only.
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

  /**
   * Clears the interrupt flag so that a cancellation raised by one test cannot surface as a
   * spurious {@link OTPRequestTimeoutException} in an unrelated test sharing the same thread.
   */
  @AfterEach
  void clearInterruptFlag() {
    Thread.interrupted();
  }

  /**
   * The walk search runs inside the snap, so a cancelled request surfaces there. A cancellation
   * carries no verdict on whether a car-reachable vertex is within the walk budget: it must be
   * raised as an exception rather than reported as a missing snap, and it must leave no verdict
   * behind that a later snap of the same vertex would read.
   */
  @Test
  void propagateCancellationInsteadOfReturningNull() {
    Thread.currentThread().interrupt();
    assertThrows(OTPRequestTimeoutException.class, () ->
      snapper.snapPickup(StreetSearchRequest.DEFAULT, A, Duration.ofMinutes(10))
    );
    Thread.interrupted();

    var result = snapper.snapPickup(StreetSearchRequest.DEFAULT, A, Duration.ofMinutes(10));
    assertNotNull(result, "A cancelled snap must leave no verdict behind");
    assertEquals(C, result.vertex());
  }

  @Test
  void alreadyCarReachableVertex_returnsSameVertexWithoutWalk() {
    var result = snapper.snapPickup(StreetSearchRequest.DEFAULT, D, Duration.ofMinutes(10));
    assertNotNull(result);
    assertEquals(D, result.vertex());
    assertNull(result.walkPath());
  }

  @Test
  void pedestrianOnlyVertex_withinBudget_snapsToNearestCarReachable() {
    var result = snapper.snapPickup(StreetSearchRequest.DEFAULT, A, Duration.ofMinutes(10));
    assertNotNull(result);
    assertEquals(C, result.vertex());
    assertNotNull(result.walkPath());
    assertTrue(result.walkPath().getDuration() > 0);
  }

  @Test
  void pedestrianOnlyVertex_budgetTooTight_returnsNull() {
    var result = snapper.snapPickup(StreetSearchRequest.DEFAULT, A, Duration.ofSeconds(5));
    assertNull(result);
  }

  @Test
  void arriveBySearch_walksBackwardsAlongIncomingEdges() {
    var result = snapper.snapDropoff(StreetSearchRequest.DEFAULT, A, Duration.ofMinutes(10));
    assertNotNull(result);
    assertEquals(C, result.vertex());
    assertNotNull(result.walkPath());
    // Walk path must be chronological: starts at C (the car-reachable vertex) and ends at A.
    assertEquals(C, result.walkPath().states.getFirst().getVertex());
    assertEquals(A, result.walkPath().states.getLast().getVertex());
    assertTrue(result.walkPath().getDuration() > 0);
  }

  /**
   * A vertex with car edges in only one direction is rejected, since a car must be able to both
   * arrive and leave. Graph: {@code S --(ped)-- V --(one-way CAR forward)--> W --(ped only)-- X
   * --(two-way CAR)-- Y}. V has outgoing CAR only, W incoming CAR only; X is the first fully
   * car-reachable vertex.
   */
  @Test
  void halfCarReachableVertexIsRejected() {
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
          // V→W forward car, reverse ped: V gets out-CAR only, W in-CAR only.
          street(
            holder[1],
            holder[2],
            50,
            StreetTraversalPermission.ALL,
            StreetTraversalPermission.PEDESTRIAN
          );
          // W↔X pedestrian both ways: walker continues, neither gains CAR.
          street(
            holder[2],
            holder[3],
            50,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          // X↔Y bidirectional car: X gets in- and out-CAR, so it is car-reachable.
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

    var result = snapper.snapPickup(StreetSearchRequest.DEFAULT, holder[0], Duration.ofMinutes(10));

    assertNotNull(result);
    assertEquals(
      holder[3],
      result.vertex(),
      "Should skip half-car-reachable V and W and snap to X"
    );
  }

  /**
   * The snap minimizes generalized walk weight, not raw distance. X is closer (50 m) but its edge
   * has {@code walkSafetyFactor=10} (weight ~500); Y is farther (100 m) at normal safety (~100), so
   * the snapper returns Y. Graph (west to east):
   * <pre>
   *   Yc --(car)-- Y --(walk 100 m, safety x1)-- S --(walk 50 m, safety x10)-- X --(car)-- Xc
   * </pre>
   * The S–Y walk weighs ~100 while the shorter S–X walk weighs ~500, so Y wins on generalized weight
   * despite being farther.
   */
  @Test
  void snapsToMinimumWeightVertex_notNearestByDistance() {
    var holder = new IntersectionVertex[5];
    modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          holder[0] = intersection("Yc", 60.0000, 9.9982);
          holder[1] = intersection("Y", 60.0000, 9.9988);
          holder[2] = intersection("S", 60.0000, 10.0000);
          holder[3] = intersection("X", 60.0000, 10.0006);
          holder[4] = intersection("Xc", 60.0000, 10.0012);

          // Yc ↔ Y gives Y bidirectional CAR access, making it car-reachable.
          street(
            holder[0],
            holder[1],
            50,
            StreetTraversalPermission.ALL,
            StreetTraversalPermission.ALL
          );

          // Y→S: longer (100 m), normal safety → weight ~100 (cheaper).
          street(
            holder[1],
            holder[2],
            100,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );

          // S→X: short (50 m) but walkSafetyFactor 10 → weight ~500.
          var sx = street(
            holder[2],
            holder[3],
            50,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          sx.forEach(e -> e.setWalkSafetyFactor(10.0f));

          // X ↔ Xc gives X bidirectional CAR access, making it car-reachable.
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

    var result = snapper.snapPickup(StreetSearchRequest.DEFAULT, holder[2], Duration.ofMinutes(10));

    assertNotNull(result);
    assertNotNull(result.walkPath(), "S is pedestrian-only — a real walk path is expected");
    assertEquals(
      holder[1],
      result.vertex(),
      "Should pick Y (low-weight, farther) over X (penalized, closer) by minimum generalized weight"
    );
  }

  /**
   * The walk A* uses the supplied request's preferences: doubling {@code walkReluctance} roughly
   * doubles the walk path's weight.
   */
  @Test
  void walkPathWeightUsesSuppliedWalkReluctance() {
    var lowReluctance = StreetSearchRequest.copyOf(StreetSearchRequest.DEFAULT)
      .withWalk(b -> b.withReluctance(1.0))
      .build();
    var highReluctance = StreetSearchRequest.copyOf(StreetSearchRequest.DEFAULT)
      .withWalk(b -> b.withReluctance(4.0))
      .build();

    var lowResult = snapper.snapPickup(lowReluctance, A, Duration.ofMinutes(10));
    var highResult = snapper.snapPickup(highReluctance, A, Duration.ofMinutes(10));

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

  /**
   * A vertex on a car network extending beyond the escape distance both ways is accepted. Graph:
   * {@code O --(ped)-- P --(car)-- P1 --(car)-- P2} (P–P1–P2 spans ~111 m); O snaps to P.
   */
  @Test
  void connectedCarVertex_isAccepted() {
    var v = new IntersectionVertex[4];
    modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          v[0] = intersection("O", 60.0000, 10.0000);
          v[1] = intersection("P", 60.0000, 10.0010);
          v[2] = intersection("P1", 60.0000, 10.0020);
          v[3] = intersection("P2", 60.0000, 10.0030);
          street(
            v[0],
            v[1],
            60,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          street(v[1], v[2], 60, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
          street(v[2], v[3], 60, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
        }
      }
    );

    var reachabilitySnapper = new CarReachableVertexSnapper(100);
    var result = reachabilitySnapper.snapPickup(
      StreetSearchRequest.DEFAULT,
      v[0],
      Duration.ofMinutes(10)
    );

    assertNotNull(result);
    assertEquals(v[1], result.vertex());
  }

  /**
   * A vertex whose only car edges form a tiny disconnected island (car "on paper" but unreachable)
   * is rejected and the snapper walks on. Graph (west to east):
   * <pre>
   *   Q2 --(car)-- Q1 --(car)-- Q --(ped)-- O --(ped)-- Isl --(car)-- IslC
   * </pre>
   * {@code Isl↔IslC} spans ~33 m (below the 100 m escape) while {@code Q–Q1–Q2} spans ~111 m, so O
   * snaps west to Q rather than to the island.
   */
  @Test
  void islandCarVertex_isRejected_snapsToReachableVertex() {
    var v = new IntersectionVertex[6];
    modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          v[0] = intersection("Q2", 60.0000, 9.9970);
          v[1] = intersection("Q1", 60.0000, 9.9980);
          v[2] = intersection("Q", 60.0000, 9.9990);
          v[3] = intersection("O", 60.0000, 10.0000);
          v[4] = intersection("Isl", 60.0000, 10.0006);
          v[5] = intersection("IslC", 60.0000, 10.0012);
          street(v[0], v[1], 60, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
          street(v[1], v[2], 60, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
          street(
            v[2],
            v[3],
            60,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          street(
            v[3],
            v[4],
            40,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          street(v[4], v[5], 40, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
        }
      }
    );

    var reachabilitySnapper = new CarReachableVertexSnapper(100);
    var result = reachabilitySnapper.snapPickup(
      StreetSearchRequest.DEFAULT,
      v[5],
      Duration.ofMinutes(10)
    );

    assertNotNull(result);
    assertEquals(
      v[2],
      result.vertex(),
      "Should skip the island Isl and snap to the connected vertex Q"
    );
  }

  /**
   * A vertex a car can be driven to but not far enough away from is rejected. Graph (west to east;
   * the car chain is one-way westbound, its reverse pedestrian):
   * <pre>
   *   Td &lt;--(car)-- T &lt;--(car)-- U1 &lt;--(car)-- U2
   *                 |
   *              (ped)
   *                 O
   * </pre>
   * The origin O is pedestrian-linked to T. A car reaches T from U2 over ~111 m (arrival passes),
   * but leaving T dead-ends at Td after only ~67 m — below the 100 m escape — so T is rejected and
   * the snap returns {@code null}.
   */
  @Test
  void deadEndTooShortToEscape_isRejected() {
    var v = new IntersectionVertex[5];
    modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          v[0] = intersection("Td", 60.0000, 9.9994);
          v[1] = intersection("O", 60.0000, 10.0000);
          v[2] = intersection("T", 60.0000, 10.0006);
          v[3] = intersection("U1", 60.0000, 10.0016);
          v[4] = intersection("U2", 60.0000, 10.0026);
          street(
            v[1],
            v[2],
            40,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          // One-way car toward T (U2→U1→T): T gains incoming car.
          street(
            v[4],
            v[3],
            60,
            StreetTraversalPermission.ALL,
            StreetTraversalPermission.PEDESTRIAN
          );
          street(
            v[3],
            v[2],
            60,
            StreetTraversalPermission.ALL,
            StreetTraversalPermission.PEDESTRIAN
          );
          // One-way car T→Td into a dead-end: T gains outgoing car that leads nowhere.
          street(
            v[2],
            v[0],
            40,
            StreetTraversalPermission.ALL,
            StreetTraversalPermission.PEDESTRIAN
          );
        }
      }
    );

    var reachabilitySnapper = new CarReachableVertexSnapper(100);
    var result = reachabilitySnapper.snapPickup(
      StreetSearchRequest.DEFAULT,
      v[1],
      Duration.ofMinutes(10)
    );

    assertNull(
      result,
      "T can be arrived at but not departed far enough; no reachable stopping point exists"
    );
  }

  /**
   * A permanent vertex's verdict depends on the permanent graph only: the probe must ignore
   * temporary edges, since a mode-blind {@code TemporaryFreeEdge} hub could offer a way out no car
   * can drive (and, once cached, outlive the hub). Graph (west to east; {@code ==temp==} is a
   * mode-blind temporary bridge):
   * <pre>
   *   M3 --(car)-- M2 --(car)-- M1 ==temp== hub ==temp== Isl --(car)-- IslC
   * </pre>
   * {@code M1–M2–M3} spans ~111 m, so M1 is genuinely car-reachable; {@code Isl↔IslC} spans only
   * ~33 m, so the island's sole escape is the temporary bridge — which the probe ignores, keeping
   * Isl rejected.
   */
  @Test
  void permanentVertexVerdict_ignoresTemporaryEdges() {
    var v = new IntersectionVertex[5];
    modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          v[0] = intersection("M3", 60.0000, 9.9974);
          v[1] = intersection("M2", 60.0000, 9.9984);
          v[2] = intersection("M1", 60.0000, 9.9994);
          v[3] = intersection("Isl", 60.0000, 10.0000);
          v[4] = intersection("IslC", 60.0000, 10.0006);
          street(v[0], v[1], 60, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
          street(v[1], v[2], 60, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
          street(v[3], v[4], 40, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);

          var hub = streetLocation("bridge", 60.0000, 9.9997);
          createTemporaryFreeEdge(v[3], hub);
          link(hub, v[2]);
          createTemporaryFreeEdge(v[2], hub);
          link(hub, v[3]);
        }
      }
    );

    var reachabilitySnapper = new CarReachableVertexSnapper(100);

    assertTrue(
      reachabilitySnapper.isCarReachable(v[2]),
      "The mainland vertex M1 is genuinely car-reachable"
    );
    assertFalse(
      reachabilitySnapper.isCarReachable(v[3]),
      "The island must stay rejected: its only way out is the mode-blind temporary bridge"
    );
  }

  /**
   * A temporary vertex on a stranded island must not be judged reachable through a foreign request's
   * mode-blind {@link org.opentripplanner.street.model.edge.TemporaryFreeEdge} bridge: the probe
   * confines traversal to its own linking, so the snap walks out to the real mainland instead. Graph
   * (west to east):
   * <pre>
   *   Q2 --(all)-- Q1 --(all)-- Q --(ped, ~56 m)-- Isl --(all, ~33 m)-- IslC
   *   Q2 =free= hub =free= IslC
   * </pre>
   * The second line is a foreign, mode-blind bridge standing in for another request's linking. The
   * island {@code Isl↔IslC} is too small to escape by car (~33 m); {@code Q–Q1–Q2} is a genuine car
   * mainland reached from Isl on foot via Q. Honouring the bridge would wrongly accept the island,
   * so the passenger (linked onto the island edge) must snap to {@code Q}.
   */
  @Test
  void temporaryVertexProbe_ignoresForeignLinking() {
    var v = new IntersectionVertex[5];
    var model = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          v[0] = intersection("Q2", 60.0000, 9.997);
          v[1] = intersection("Q1", 60.0000, 9.998);
          v[2] = intersection("Q", 60.0000, 9.999);
          v[3] = intersection("Isl", 60.0000, 10.0000);
          v[4] = intersection("IslC", 60.0000, 10.0006);
          street(v[0], v[1], 56, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
          street(v[1], v[2], 56, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
          street(
            v[2],
            v[3],
            56,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          street(v[3], v[4], 33, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
          // Foreign mode-blind bridge IslC <-> hub <-> Q2, standing in for another request's linking.
          var hub = streetLocation("foreign-bridge", 60.0000, 10.0009);
          link(v[4], hub);
          link(hub, v[4]);
          link(hub, v[0]);
          link(v[0], hub);
        }
      }
    );

    var vertexCreationService = new VertexCreationService(
      VertexLinkerTestFactory.of(model.graph())
    );
    var reachabilitySnapper = new CarReachableVertexSnapper(100);

    try (var container = new TemporaryVerticesContainer()) {
      var passenger = new StreetVertexUtils(vertexCreationService, container).createPassengerVertex(
        new WgsCoordinate(60.0000, 10.0003)
      );
      assertNotNull(passenger);

      var result = reachabilitySnapper.snapPickup(
        StreetSearchRequest.DEFAULT,
        passenger,
        Duration.ofMinutes(10)
      );

      assertNotNull(result);
      assertEquals(
        v[2],
        result.vertex(),
        "Must skip the foreign bridge and walk out to the reachable mainland vertex Q"
      );
    }
  }

  /**
   * {@code snapToPermanentVertex} never accepts a temporary vertex, even when it is the cheapest
   * car-reachable one, while the plain snap does. A coordinate linked mid-edge on
   * {@code A --(100 m)-- B} produces exactly that: the splitter vertices are the nearest
   * car-reachable ones but are temporary, so only an edge endpoint may be stored.
   */
  @Test
  void snapToPermanentVertex_skipsTemporaryVertices() {
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
    var reachabilitySnapper = new CarReachableVertexSnapper(50);

    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var linked = new StreetVertexUtils(
        vertexCreationService,
        temporaryVerticesContainer
      ).createDriverWaypointVertex(new WgsCoordinate(60.0000, 10.0005));
      assertNotNull(linked);

      var plain = reachabilitySnapper.snapPickup(
        StreetSearchRequest.DEFAULT,
        linked,
        Duration.ofMinutes(10)
      );
      var permanent = reachabilitySnapper.snapToPermanentVertex(
        StreetSearchRequest.DEFAULT,
        linked,
        Duration.ofMinutes(10)
      );

      assertNotNull(plain);
      assertTrue(
        plain.vertex() instanceof TemporaryVertex,
        "The plain snap accepts the linking's own splitter vertex"
      );
      assertNotNull(permanent);
      assertEquals(
        v[0],
        permanent.vertex(),
        "The permanent snap must walk on to the nearer edge endpoint"
      );
    }
  }
}
