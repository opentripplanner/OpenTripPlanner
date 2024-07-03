package org.opentripplanner.astar.strategy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.impl.BatteryValidator;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

public class BatteryDistanceSkipEdgeStrategyTest extends GraphRoutingTest {

  /**
   * battery is not enough for driven distance-> skips Edge
   * battery is 0m, driven meters = 100 => true
   */
  @Test
  void batteryIsNotEnough() {
    var state = getState(100.0);

    state.currentRangeMeters = Optional.of(0.0);

    var strategy = new BatteryDistanceSkipEdgeStrategy(BatteryValidator::wouldBatteryRunOut);

    assertTrue(strategy.shouldSkipEdge(state, null));
  }

  /**
   * battery is enough for driven distance-> does not skip Edge
   * battery is 4000m, driven meters = 100 => false
   */
  @Test
  void batteryIsEnough() {
    var state = getState(100.0);
    state.currentRangeMeters = Optional.of(4000.0);

    var strategy = new BatteryDistanceSkipEdgeStrategy(BatteryValidator::wouldBatteryRunOut);
    assertFalse(strategy.shouldSkipEdge(state, null));
  }

  /**
   * battery dies at exact time location is reached -> does not skip edge
   * battery is 100m, driven meters = 100 => false
   */
  @Test
  void batteryDiesAtFinalLocation() {
    var state = getState(100.0);

    state.currentRangeMeters = Optional.of(100.0);

    var strategy = new BatteryDistanceSkipEdgeStrategy(BatteryValidator::wouldBatteryRunOut);
    assertFalse(strategy.shouldSkipEdge(state, null));
  }

  /**
   * battery has remaining Energy, no distance was driven so far -> does not skip edge
   * battery is 100m, driven meters = 0 => false
   */
  @Test
  void noDrivenMeters() {
    var state = getState(0.0);
    state.currentRangeMeters = Optional.of(100.0);

    var strategy = new BatteryDistanceSkipEdgeStrategy(BatteryValidator::wouldBatteryRunOut);
    assertFalse(strategy.shouldSkipEdge(state, null));
  }

  /**
   * vehicle.currentRangeMeters (battery) is Null or of Empty Value -> does not skip Edge
   * Battery is Optional.Empty() => false
   */
  @Test
  void batteryHasNoValue() {
    var state = TestStateBuilder.ofScooterRental().build();
    state.currentRangeMeters = Optional.empty();

    var strategy = new BatteryDistanceSkipEdgeStrategy(BatteryValidator::wouldBatteryRunOut);
    assertFalse(strategy.shouldSkipEdge(state, null));
  }

  private static State getState(double drivenBatteryMeters) {
    var state = TestStateBuilder.ofScooterRental().build();
    state.drivenBatteryMeters = drivenBatteryMeters;
    return state;
  }
}
