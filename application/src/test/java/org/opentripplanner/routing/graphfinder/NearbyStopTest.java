package org.opentripplanner.routing.graphfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class NearbyStopTest {

  private static final TimetableRepositoryForTest MODEL = TimetableRepositoryForTest.of();
  private static final Instant START_TIME = Instant.parse("2024-01-15T12:00:00Z");

  @Test
  void testIsBetter() {
    // We only test the distance here, since the compareTo method used should have a more complete
    // unit-test including tests on state weight.
    var a = new NearbyStop(MODEL.stop("A").build(), 20.0, null, null);
    var b = new NearbyStop(MODEL.stop("A").build(), 30.0, null, null);

    assertTrue(a.isBetter(b));
    assertFalse(b.isBetter(a));

    var sameDistance = new NearbyStop(MODEL.stop("A").build(), 20.0, null, null);
    assertFalse(a.isBetter(sameDistance));
    assertFalse(sameDistance.isBetter(a));
  }

  @Test
  void nearbyStopForStateDepartAfter() {
    var graph = createThreeEdgeGraph();
    var state = walkForward(graph);

    var nearbyStop = NearbyStop.nearbyStopForState(state, MODEL.stop("S").build());

    assertEquals(3, nearbyStop.edges.size());
    assertSame(graph.e1, nearbyStop.edges.get(0));
    assertSame(graph.e2, nearbyStop.edges.get(1));
    assertSame(graph.e3, nearbyStop.edges.get(2));
    assertTrue(nearbyStop.distance > 0);
  }

  @Test
  void nearbyStopForStateArriveBy() {
    var graph = createThreeEdgeGraph();
    var state = walkBackward(graph);

    var nearbyStop = NearbyStop.nearbyStopForState(state, MODEL.stop("S").build());

    assertEquals(3, nearbyStop.edges.size());
    assertSame(graph.e1, nearbyStop.edges.get(0));
    assertSame(graph.e2, nearbyStop.edges.get(1));
    assertSame(graph.e3, nearbyStop.edges.get(2));
    assertTrue(nearbyStop.distance > 0);
  }

  @Test
  void nearbyStopForStateWithNoEdges() {
    var v1 = StreetModelForTest.intersectionVertex("NearbyStopTest_solo", 59.910, 10.750);
    var request = StreetSearchRequest.of()
      .withMode(StreetMode.WALK)
      .withArriveBy(false)
      .withStartTime(START_TIME)
      .build();
    var s0 = new State(v1, request);

    var nearbyStop = NearbyStop.nearbyStopForState(s0, MODEL.stop("S").build());

    assertEquals(0, nearbyStop.edges.size());
    assertEquals(0.0, nearbyStop.distance, 1e-9);
  }

  @Test
  void nearbyStopForStateBothDirectionsProduceIdenticalEdges() {
    var graph = createThreeEdgeGraph();
    var stop = MODEL.stop("S").build();

    var forward = NearbyStop.nearbyStopForState(walkForward(graph), stop);
    var backward = NearbyStop.nearbyStopForState(walkBackward(graph), stop);

    assertEquals(forward.edges, backward.edges);
    assertEquals(forward.distance, backward.distance, 1e-9);
  }

  /**
   * Build a forward (depart-after) state chain: V1 → e1 → V2 → e2 → V3 → e3 → V4.
   */
  private State walkForward(ThreeEdgeGraph g) {
    var request = StreetSearchRequest.of()
      .withMode(StreetMode.WALK)
      .withArriveBy(false)
      .withStartTime(START_TIME)
      .build();
    var s0 = new State(g.v1, request);
    var s1 = g.e1.traverse(s0)[0];
    var s2 = g.e2.traverse(s1)[0];
    var s3 = g.e3.traverse(s2)[0];
    return s3;
  }

  /**
   * Build an arriveBy state chain: start at V4, traverse e3 backward → V3 → e2 → V2 → e1 → V1.
   */
  private State walkBackward(ThreeEdgeGraph g) {
    var request = StreetSearchRequest.of()
      .withMode(StreetMode.WALK)
      .withArriveBy(true)
      .withStartTime(START_TIME)
      .build();
    var s0 = new State(g.v4, request);
    var s1 = g.e3.traverse(s0)[0];
    var s2 = g.e2.traverse(s1)[0];
    var s3 = g.e1.traverse(s2)[0];
    return s3;
  }

  private static ThreeEdgeGraph createThreeEdgeGraph() {
    var v1 = StreetModelForTest.intersectionVertex("NearbyStopTest_1", 59.910, 10.750);
    var v2 = StreetModelForTest.intersectionVertex("NearbyStopTest_2", 59.911, 10.751);
    var v3 = StreetModelForTest.intersectionVertex("NearbyStopTest_3", 59.912, 10.752);
    var v4 = StreetModelForTest.intersectionVertex("NearbyStopTest_4", 59.913, 10.753);
    var e1 = StreetModelForTest.streetEdge(v1, v2);
    var e2 = StreetModelForTest.streetEdge(v2, v3);
    var e3 = StreetModelForTest.streetEdge(v3, v4);
    return new ThreeEdgeGraph(v1, v2, v3, v4, e1, e2, e3);
  }

  private record ThreeEdgeGraph(
    StreetVertex v1,
    StreetVertex v2,
    StreetVertex v3,
    StreetVertex v4,
    StreetEdge e1,
    StreetEdge e2,
    StreetEdge e3
  ) {}
}
