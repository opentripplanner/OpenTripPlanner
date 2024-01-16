package org.opentripplanner.ext.restapi.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model.basic.TransitMode.CARPOOL;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.api.parameter.ApiRequestMode;

class ApiRequestModeTest {

  @Test
  void carpool() {
    assertEquals(List.of(CARPOOL), ApiRequestMode.CARPOOL.getTransitModes());
  }
}
