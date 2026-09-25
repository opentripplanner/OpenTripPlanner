package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

/** A --- B on foot or by bike, B --- C by bike only. */
class GraphPathReplayTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(63.43, 10.39);

  private IntersectionVertex a;
  private IntersectionVertex b;
  private IntersectionVertex c;

  @BeforeEach
  void setUp() {
    modelOf(
      new Builder() {
        @Override
        public void build() {
          a = intersection("A", ORIGIN);
          b = intersection("B", ORIGIN.moveEastMeters(100));
          c = intersection("C", ORIGIN.moveEastMeters(200));
          street(
            a,
            b,
            100,
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
          );
          street(b, c, 100, StreetTraversalPermission.BICYCLE, StreetTraversalPermission.BICYCLE);
        }
      }
    );
  }

  @Test
  void replayKeepsTheEdgesAndAppliesTheRequestsWalkingSpeed() {
    var walk = path(StreetMode.WALK, a, b);
    var slow = StreetSearchRequest.copyOf(StreetSearchRequest.DEFAULT)
      .withWalk(w -> w.withSpeed(StreetSearchRequest.DEFAULT.walk().speed() / 2))
      .build();

    var replayed = GraphPathUtils.replay(walk, slow);

    assertNotNull(replayed);
    assertEquals(walk.edges, replayed.edges);
    assertSame(a, replayed.states.getFirst().getVertex());
    assertSame(b, replayed.states.getLast().getVertex());
    // The preference rounds the speed, so compare against the ratio the request ended up with.
    double ratio = StreetSearchRequest.DEFAULT.walk().speed() / slow.walk().speed();
    assertTrue(
      Math.abs(replayed.getDuration() - ratio * walk.getDuration()) <= 2,
      "slower walk, longer time: " + walk.getDuration() + " -> " + replayed.getDuration()
    );
  }

  @Test
  void replayIsNullWhenAnEdgeCannotBeWalked() {
    var byBike = path(StreetMode.BIKE, a, c);
    assertNull(GraphPathUtils.replay(byBike, StreetSearchRequest.DEFAULT));
  }

  private GraphPath<State, Edge, Vertex> path(StreetMode mode, Vertex from, Vertex to) {
    var paths = StreetSearchBuilder.of()
      .withRequest(StreetSearchRequest.of().withMode(mode).build())
      .withFrom(from)
      .withTo(to)
      .getPathsToTarget();
    assertEquals(1, paths.size(), "a path " + from + " -> " + to + " by " + mode);
    return paths.getFirst().toGraphPath();
  }
}
