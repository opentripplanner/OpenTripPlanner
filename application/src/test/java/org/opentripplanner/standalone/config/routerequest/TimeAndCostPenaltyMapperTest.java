package org.opentripplanner.standalone.config.routerequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import org.junit.jupiter.api.Test;

class TimeAndCostPenaltyMapperTest {

  @Test
  void mapNormal() {
    var node = newNodeAdapterForTest(
      """
      { "timePenalty": "2m + 3t", "costFactor": 3.4 }
      """
    );
    assertEquals(
      "(timePenalty: 2m + 3.0 t, costFactor: 3.4)",
      TimeAndCostPenaltyMapper.map(node).toString()
    );
  }

  @Test
  void mapMissingTimePenalty() {
    var node = newNodeAdapterForTest(
      """
      { "costFactor": 3.4 }
      """
    );
    var ex = assertThrows(IllegalArgumentException.class, () -> TimeAndCostPenaltyMapper.map(node));
    assertEquals(
      "When time-penalty is zero, the costFactor have no effect and should be zero as well.",
      ex.getMessage()
    );
  }
}
