package org.opentripplanner.ext.carpooling.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolingParameters;

class CarpoolingConfigTest {

  @Test
  void defaultsWhenAbsent() {
    var root = newNodeAdapterForTest("{}");
    assertEquals(CarpoolingParameters.DEFAULT, CarpoolingConfig.mapParameters("carpooling", root));
  }

  @Test
  void boardCost() {
    var root = newNodeAdapterForTest("{ carpooling: { boardCost: 1200 } }");
    assertEquals(1200, CarpoolingConfig.mapParameters("carpooling", root).boardCost());
  }
}
