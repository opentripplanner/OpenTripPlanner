package org.opentripplanner.ext.flex.flexpathcalculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.search.state.TestStateBuilder.ofDriving;

import org.junit.jupiter.api.Test;

class StateToFlexPathMapperTest {

  private static final int EPSILON = 10;
  private static final int EXPECTED_DISTANCE = 471_509;
  private static final int EXPECTED_DURATION = 42_100;

  @Test
  void departAt() {
    var state = ofDriving().streetEdge().streetEdge().streetEdge().build();
    var flexPath = StateToFlexPathMapper.map(state);
    assertEquals(EXPECTED_DISTANCE, flexPath.distanceMeters, EPSILON);
    assertEquals(EXPECTED_DURATION, flexPath.durationSeconds, EPSILON);
    assertEquals("LINESTRING (1 1, 2 2, 3 3, 4 4)", flexPath.getGeometry().toString());
  }

  @Test
  void arriveBy() {
    var state = ofDriving().streetEdge().streetEdge().streetEdge().build().reverse();
    var flexPath = StateToFlexPathMapper.map(state);
    assertEquals(EXPECTED_DISTANCE, flexPath.distanceMeters, EPSILON);
    assertEquals(EXPECTED_DURATION, flexPath.durationSeconds, EPSILON);
    assertEquals("LINESTRING (1 1, 2 2, 3 3, 4 4)", flexPath.getGeometry().toString());
  }
}
