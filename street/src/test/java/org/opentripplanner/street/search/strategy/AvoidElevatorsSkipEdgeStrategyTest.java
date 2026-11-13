package org.opentripplanner.street.search.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelFactory.streetEdge;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

class AvoidElevatorsSkipEdgeStrategyTest {

  private final StreetVertex from = intersectionVertex(0, 0);
  private final StreetVertex to = intersectionVertex(1, 1);
  private final State state = new State(from, StreetSearchRequest.of().build());

  @Test
  void skipsAnElevatorWithAnIdToAvoid() {
    var strategy = new AvoidElevatorsSkipEdgeStrategy(Set.of("e1", "e2"));

    assertTrue(strategy.shouldSkipEdge(state, elevator("e1")));
    assertTrue(strategy.shouldSkipEdge(state, elevator("e2")));
  }

  @Test
  void doesNotSkipAnElevatorWithAnotherId() {
    var strategy = new AvoidElevatorsSkipEdgeStrategy(Set.of("e1"));

    assertFalse(strategy.shouldSkipEdge(state, elevator("e3")));
  }

  @Test
  void doesNotSkipAnElevatorWithoutAnId() {
    var strategy = new AvoidElevatorsSkipEdgeStrategy(Set.of("e1"));
    var noId = ElevatorHopEdge.createElevatorHopEdge(
      from,
      to,
      StreetTraversalPermission.ALL,
      Accessibility.POSSIBLE,
      1,
      20
    );

    assertFalse(strategy.shouldSkipEdge(state, noId));
  }

  @Test
  void doesNotSkipAnEdgeThatIsNotAnElevator() {
    var strategy = new AvoidElevatorsSkipEdgeStrategy(Set.of("e1"));

    assertFalse(strategy.shouldSkipEdge(state, streetEdge(intersectionVertex(2, 2), to)));
  }

  @Test
  void doesNotSkipAnythingIfThereIsNothingToAvoid() {
    var strategy = new AvoidElevatorsSkipEdgeStrategy(Set.of());

    assertFalse(strategy.shouldSkipEdge(state, elevator("e1")));
  }

  @Test
  void toStringListsTheElevatorIds() {
    assertEquals(
      "AvoidElevatorsSkipEdgeStrategy{elevatorIds: [e1]}",
      new AvoidElevatorsSkipEdgeStrategy(Set.of("e1")).toString()
    );
  }

  private Edge elevator(String id) {
    ElevatorHopEdge.bidirectional(
      from,
      to,
      StreetTraversalPermission.ALL,
      Accessibility.POSSIBLE,
      1,
      20,
      id
    );
    return from
      .getOutgoing()
      .stream()
      .filter(it -> it instanceof ElevatorHopEdge e && e.id().filter(id::equals).isPresent())
      .findFirst()
      .orElseThrow();
  }
}
