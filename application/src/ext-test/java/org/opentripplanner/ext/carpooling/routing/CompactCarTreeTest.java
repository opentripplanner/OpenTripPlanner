package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;

/**
 * The compact tree must come out with the same travel times as the generic street search it
 * replaces, on every feature of the street model a car search touches: turn costs at
 * intersections, one-way streets, streets a car may not use, no-through-traffic pockets, the free
 * edges of temporary locations, and transit stop links.
 * <p>
 * Graph (not to scale; irregular spacing so that no two routes tie on time):
 * <pre>
 *   row 900 m:  G02 --- G12 --- G22 --> X (one-way)
 *                :               |
 *   pocket N1 - N2 (no-thru)     |        P (pedestrian street off G20)
 *                                |
 *   row 400 m:  G01 --- G11 --- G21 = S (transit stop)
 *                        :
 *                        T (temporary location, free edges)
 *   row   0 m:  G00 --- G10 --- G20 --- P
 *   columns:     0      300     700 m
 * </pre>
 * The pocket N1-N2 hangs between G02 and G12 on no-through-traffic streets that are much shorter
 * than the G02-G12 street: a search that let cars pass through would find a faster G02 → G12.
 */
class CompactCarTreeTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(63.43, 10.39);
  private static final Duration UNLIMITED = Duration.ofHours(1);

  private final List<Vertex> streetVertices = new ArrayList<>();
  private IntersectionVertex g00;
  private IntersectionVertex g22;
  private IntersectionVertex g02;
  private IntersectionVertex g12;
  private IntersectionVertex x;
  private IntersectionVertex p;
  private IntersectionVertex n1;
  private IntersectionVertex n2;
  private TemporaryStreetLocation t;
  private TransitStopVertex stop;

  @BeforeEach
  void setUp() {
    modelOf(
      new Builder() {
        @Override
        public void build() {
          int[] columns = { 0, 300, 700 };
          int[] rows = { 0, 400, 900 };
          var grid = new IntersectionVertex[3][3];
          for (int c = 0; c < 3; c++) {
            for (int r = 0; r < 3; r++) {
              grid[c][r] = intersection(
                "G" + c + r,
                ORIGIN.moveEastMeters(columns[c]).moveNorthMeters(rows[r])
              );
              streetVertices.add(grid[c][r]);
            }
          }
          for (int c = 0; c < 3; c++) {
            for (int r = 0; r < 3; r++) {
              if (c < 2) {
                biStreet(grid[c][r], grid[c + 1][r], columns[c + 1] - columns[c]);
              }
              if (r < 2) {
                biStreet(grid[c][r], grid[c][r + 1], rows[r + 1] - rows[r]);
              }
            }
          }
          g00 = grid[0][0];
          g02 = grid[0][2];
          g12 = grid[1][2];
          g22 = grid[2][2];

          // One-way street out of the north-east corner.
          x = intersection("X", ORIGIN.moveEastMeters(1100).moveNorthMeters(900));
          street(g22, x, 400, StreetTraversalPermission.ALL);
          streetVertices.add(x);

          // A street cars may not use.
          p = intersection("P", ORIGIN.moveEastMeters(1000));
          street(
            grid[2][0],
            p,
            300,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          streetVertices.add(p);

          // A no-through-traffic pocket that would be a shortcut between G02 and G12.
          n1 = intersection("N1", ORIGIN.moveEastMeters(100).moveNorthMeters(1000));
          n2 = intersection("N2", ORIGIN.moveEastMeters(200).moveNorthMeters(1000));
          noThru(g02, n1, 60);
          noThru(n1, n2, 60);
          noThru(n2, g12, 60);
          streetVertices.add(n1);
          streetVertices.add(n2);

          // A temporary location hanging off G11 by free edges, like a linked trip waypoint.
          t = streetLocation("T", grid[1][1].getLat() - 0.0005, grid[1][1].getLon());
          link(t, grid[1][1]);
          link(grid[1][1], t);

          // A transit stop linked to G21.
          stop = stop("S", grid[2][1].toWgsCoordinate());
          biLink(grid[2][1], stop);
        }

        private void noThru(IntersectionVertex a, IntersectionVertex b, int length) {
          streetBuilder(a, b, length, StreetTraversalPermission.ALL)
            .withMotorVehicleNoThruTraffic(true)
            .buildAndConnect();
          streetBuilder(b, a, length, StreetTraversalPermission.ALL)
            .withMotorVehicleNoThruTraffic(true)
            .buildAndConnect();
        }
      }
    );
  }

  @Test
  void forwardTreeMatchesTheGenericSearch() {
    assertMatchesReference(g00, false, UNLIMITED);
    assertMatchesReference(t, false, UNLIMITED);
  }

  @Test
  void reverseTreeMatchesTheGenericSearch() {
    assertMatchesReference(g22, true, UNLIMITED);
    assertMatchesReference(t, true, UNLIMITED);
  }

  @Test
  void durationLimitMatchesTheGenericSearch() {
    // Tight enough to cut the grid in half, so the limit itself is what is being compared.
    var limit = Duration.ofSeconds(45);
    assertMatchesReference(g00, false, limit);
    assertMatchesReference(g22, true, limit);
  }

  @Test
  void pedestrianStreetIsNotDriven() {
    var tree = CompactCarTree.build(g00, false, UNLIMITED, null);
    assertEquals(-1, tree.elapsedSeconds(p));
  }

  @Test
  void oneWayStreetIsDrivenInItsDirectionOnly() {
    var forward = CompactCarTree.build(g00, false, UNLIMITED, null);
    assertTrue(forward.elapsedSeconds(x) > 0, "X is reachable along the one-way street");

    var reverseFromX = CompactCarTree.build(x, false, UNLIMITED, null);
    assertEquals(-1, reverseFromX.elapsedSeconds(g22), "X is a dead end for a car");
  }

  @Test
  void noThroughTrafficPocketIsNoShortcut() {
    var tree = CompactCarTree.build(g02, false, UNLIMITED, null);
    // The pocket itself may be entered ...
    assertTrue(tree.elapsedSeconds(n1) > 0);
    assertTrue(tree.elapsedSeconds(n2) > 0);
    // ... but G12 must be reached along the normal street, not through the pocket, which would
    // take 180 m instead of 300 m.
    int viaPocket = tree.elapsedSeconds(n2) + secondsFor(60);
    assertTrue(
      tree.elapsedSeconds(g12) > viaPocket,
      "G12 reached in " + tree.elapsedSeconds(g12) + "s, the pocket would give " + viaPocket
    );
  }

  @Test
  void transitStopVerticesAreNotEntered() {
    var tree = CompactCarTree.build(g00, false, UNLIMITED, null);
    assertEquals(-1, tree.elapsedSeconds(stop));
  }

  @Test
  void pathReplaysTheSearchInBothDirections() {
    var forward = CompactCarTree.build(t, false, UNLIMITED, null);
    var path = forward.path(x);
    assertNotNull(path);
    assertSame(t, path.states.getFirst().getVertex());
    assertSame(x, path.states.getLast().getVertex());
    assertEquals(forward.elapsedSeconds(x), path.getDuration());
    assertEquals(path.states.size() - 1, path.edges.size());

    var reverse = CompactCarTree.build(t, true, UNLIMITED, null);
    var reversePath = reverse.path(g02);
    assertNotNull(reversePath);
    assertSame(g02, reversePath.states.getFirst().getVertex(), "chronological: starts at G02");
    assertSame(t, reversePath.states.getLast().getVertex(), "chronological: ends at the root");
    assertEquals(reverse.elapsedSeconds(g02), reversePath.getDuration());
    assertTrue(
      reversePath.states.getFirst().getTimeSeconds() < reversePath.states.getLast().getTimeSeconds()
    );
  }

  @Test
  void pathToUnreachedVertexIsNull() {
    var tree = CompactCarTree.build(g00, false, UNLIMITED, null);
    assertNull(tree.path(p));
  }

  @Test
  void ellipseBoundsKeepTheLegAndDropTheRest() {
    var unbounded = CompactCarTree.build(g00, false, UNLIMITED, null);
    long legSeconds = unbounded.elapsedSeconds(g22);
    // The bound admits the leg with 5 s to spare; X lies beyond G22 and can never lead back to it
    // within that, while everything on a fastest route to G22 stays.
    var bounds = new EllipseBounds(
      List.of(new EllipseBounds.Focus(g22.getCoordinate(), legSeconds + 5)),
      40.0
    );
    var bounded = CompactCarTree.build(g00, false, UNLIMITED, bounds);

    assertEquals(unbounded.elapsedSeconds(g22), bounded.elapsedSeconds(g22));
    assertEquals(-1, bounded.elapsedSeconds(x));
    assertTrue(bounded.size() < unbounded.size());
  }

  /* ------------------------------------------------------------------ helpers */

  private void assertMatchesReference(Vertex root, boolean reverse, Duration limit) {
    var compact = CompactCarTree.build(root, reverse, limit, null);
    var reference = referenceTree(root, reverse, limit);
    var targets = new ArrayList<>(streetVertices);
    targets.add(t);
    int reached = 0;
    for (Vertex vertex : targets) {
      var state = reference.getState(vertex);
      int expected = state == null ? -1 : (int) state.getElapsedTimeSeconds();
      assertEquals(
        expected,
        compact.elapsedSeconds(vertex),
        (reverse ? "reverse" : "forward") + " tree from " + root + " at " + vertex
      );
      if (expected >= 0) {
        reached++;
      }
    }
    assertTrue(reached > 1, "the comparison must cover reached vertices");
  }

  private static ShortestPathTree<State, Edge, Vertex> referenceTree(
    Vertex root,
    boolean reverse,
    Duration limit
  ) {
    var request = reverse
      ? StreetSearchRequest.of().withMode(StreetMode.CAR).withArriveBy(true).build()
      : StreetSearchRequest.of().withMode(StreetMode.CAR).build();
    var builder = StreetSearchBuilder.of()
      .withSkipEdgeStrategy(new DurationSkipEdgeStrategy<>(limit))
      .withDominanceFunction(new DominanceFunctions.EarliestArrival())
      .withRequest(request);
    return reverse
      ? builder.withTo(root).getShortestPathTree()
      : builder.withFrom(root).getShortestPathTree();
  }

  /** Whole seconds a car needs for {@code meters} of test street at the builder's default speed. */
  private int secondsFor(int meters) {
    // Read the speed off an actual edge rather than assuming the builder's default.
    var edge = (org.opentripplanner.street.model.edge.StreetEdge) g00
      .getOutgoing()
      .iterator()
      .next();
    return (int) Math.floor(meters / edge.getCarSpeed());
  }
}
