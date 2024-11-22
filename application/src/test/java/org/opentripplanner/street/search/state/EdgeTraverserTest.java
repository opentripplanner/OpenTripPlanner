package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class EdgeTraverserTest {

  private static final IntersectionVertex BERLIN_V = intersectionVertex(Coordinates.BERLIN);
  private static final IntersectionVertex BRANDENBURG_GATE_V = intersectionVertex(
    Coordinates.BERLIN_BRANDENBURG_GATE
  );
  private static final IntersectionVertex FERNSEHTURM_V = intersectionVertex(
    Coordinates.BERLIN_FERNSEHTURM
  );
  private static final IntersectionVertex ADMIRALBRUCKE_V = intersectionVertex(
    Coordinates.BERLIN_ADMIRALBRUCKE
  );

  @Test
  void emptyEdges() {
    var request = StreetSearchRequest
      .of()
      .withStartTime(Instant.EPOCH)
      .withMode(StreetMode.WALK)
      .build();
    var initialStates = State.getInitialStates(Set.of(BERLIN_V), request);
    var traversedState = EdgeTraverser.traverseEdges(initialStates, List.of());

    assertSame(initialStates.iterator().next(), traversedState.get());
  }

  @Test
  void failedTraversal() {
    var edge = StreetModelForTest
      .streetEdge(BERLIN_V, BRANDENBURG_GATE_V)
      .toBuilder()
      .withPermission(StreetTraversalPermission.NONE)
      .buildAndConnect();

    var edges = List.<Edge>of(edge);
    var request = StreetSearchRequest
      .of()
      .withStartTime(Instant.EPOCH)
      .withMode(StreetMode.WALK)
      .build();
    var initialStates = State.getInitialStates(Set.of(edge.getFromVertex()), request);
    var traversedState = EdgeTraverser.traverseEdges(initialStates, edges);

    assertTrue(traversedState.isEmpty());
  }

  @Test
  void withSingleState() {
    var edge = StreetModelForTest
      .streetEdge(BERLIN_V, BRANDENBURG_GATE_V)
      .toBuilder()
      .withPermission(StreetTraversalPermission.ALL)
      .buildAndConnect();

    var edges = List.<Edge>of(edge);
    var request = StreetSearchRequest
      .of()
      .withStartTime(Instant.EPOCH)
      .withMode(StreetMode.WALK)
      .build();
    var initialStates = State.getInitialStates(Set.of(edge.getFromVertex()), request);
    var traversedState = EdgeTraverser.traverseEdges(initialStates, edges).get();

    assertEquals(List.of(TraverseMode.WALK), stateValues(traversedState, State::getBackMode));
    assertEquals(1719, traversedState.getElapsedTimeSeconds());
  }

  @Test
  void withSingleArriveByState() {
    var edge = StreetModelForTest
      .streetEdge(BERLIN_V, BRANDENBURG_GATE_V)
      .toBuilder()
      .withPermission(StreetTraversalPermission.ALL)
      .buildAndConnect();

    var edges = List.<Edge>of(edge);
    var request = StreetSearchRequest
      .of()
      .withStartTime(Instant.EPOCH)
      .withMode(StreetMode.WALK)
      .withArriveBy(true)
      .build();
    var initialStates = State.getInitialStates(Set.of(edge.getToVertex()), request);
    var traversedState = EdgeTraverser.traverseEdges(initialStates, edges).get();

    assertSame(BERLIN_V, traversedState.getVertex());
    assertEquals(List.of(TraverseMode.WALK), stateValues(traversedState, State::getBackMode));
    assertEquals(1719, traversedState.getElapsedTimeSeconds());
  }

  @Test
  void withMultipleStates() {
    // CAR_PICKUP creates parallel walking and driving states
    // This tests that of the two states (WALKING, CAR) the least weight (CAR) is selected
    var edge = StreetModelForTest
      .streetEdge(BERLIN_V, BRANDENBURG_GATE_V)
      .toBuilder()
      .withPermission(StreetTraversalPermission.ALL)
      .buildAndConnect();

    var edges = List.<Edge>of(edge);
    var request = StreetSearchRequest
      .of()
      .withStartTime(Instant.EPOCH)
      .withMode(StreetMode.CAR_PICKUP)
      .build();
    var initialStates = State.getInitialStates(Set.of(edge.getFromVertex()), request);
    var traversedState = EdgeTraverser.traverseEdges(initialStates, edges).get();

    assertEquals(List.of(TraverseMode.CAR), stateValues(traversedState, State::getBackMode));
    assertEquals(205, traversedState.getElapsedTimeSeconds());
  }

  @Test
  void withDominatedStates() {
    // CAR_PICKUP creates parallel walking and driving states
    // This tests that the most optimal (walking and driving the last stretch) is found after
    // discarding the initial driving state for edge1
    var edge1 = StreetModelForTest
      .streetEdge(FERNSEHTURM_V, BERLIN_V)
      .toBuilder()
      .withPermission(StreetTraversalPermission.ALL)
      .buildAndConnect();
    var edge2 = StreetModelForTest
      .streetEdge(BERLIN_V, BRANDENBURG_GATE_V)
      .toBuilder()
      .withPermission(StreetTraversalPermission.PEDESTRIAN)
      .buildAndConnect();
    var edge3 = StreetModelForTest
      .streetEdge(BRANDENBURG_GATE_V, ADMIRALBRUCKE_V)
      .toBuilder()
      .withPermission(StreetTraversalPermission.ALL)
      .buildAndConnect();

    var edges = List.<Edge>of(edge1, edge2, edge3);
    var request = StreetSearchRequest
      .of()
      .withStartTime(Instant.EPOCH)
      .withMode(StreetMode.CAR_PICKUP)
      .build();
    var initialStates = State.getInitialStates(Set.of(edge1.getFromVertex()), request);
    var traversedState = EdgeTraverser.traverseEdges(initialStates, edges).get();

    assertEquals(
      List.of(88.103, 2286.029, 3444.28),
      stateValues(
        traversedState,
        state -> state.getBackEdge() != null ? state.getBackEdge().getDistanceMeters() : null
      )
    );
    assertEquals(
      List.of(TraverseMode.WALK, TraverseMode.WALK, TraverseMode.CAR),
      stateValues(traversedState, State::getBackMode)
    );
    assertEquals(2169, traversedState.getElapsedTimeSeconds());
  }

  private <T> List<T> stateValues(State state, Function<State, T> extractor) {
    var values = new ArrayList<T>();
    while (state != null) {
      var value = extractor.apply(state);
      if (value != null) {
        values.add(value);
      }
      state = state.getBackState();
    }
    return values.reversed();
  }
}
