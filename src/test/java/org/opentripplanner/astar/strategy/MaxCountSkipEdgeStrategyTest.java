package org.opentripplanner.astar.strategy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.street.search.state.TestStateBuilder;

class MaxCountSkipEdgeStrategyTest {

  @Test
  void countStops() {
    var state = TestStateBuilder.ofWalking().stop().build();
    var strategy = new MaxCountSkipEdgeStrategy<>(1, NearbyStopFinder::hasReachedStop);
    assertFalse(strategy.shouldSkipEdge(state, null));
    assertTrue(strategy.shouldSkipEdge(state, null));
  }

  @Test
  void doNotCountStop() {
    var state = TestStateBuilder.ofWalking().build();
    var strategy = new MaxCountSkipEdgeStrategy<>(1, NearbyStopFinder::hasReachedStop);
    assertFalse(strategy.shouldSkipEdge(state, null));
    assertFalse(strategy.shouldSkipEdge(state, null));
    assertFalse(strategy.shouldSkipEdge(state, null));
  }

  @Test
  void nonFinalState() {
    var state = TestStateBuilder.ofScooterRentalArriveBy().stop().build();
    assertFalse(state.isFinal());
    var strategy = new MaxCountSkipEdgeStrategy<>(1, NearbyStopFinder::hasReachedStop);
    assertFalse(strategy.shouldSkipEdge(state, null));
  }
}
