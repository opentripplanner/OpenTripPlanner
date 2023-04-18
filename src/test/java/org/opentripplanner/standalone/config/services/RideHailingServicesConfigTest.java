package org.opentripplanner.standalone.config.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromResource;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.ridehailing.RideHailingServiceParameters;
import org.opentripplanner.standalone.config.RouterConfig;

public class RideHailingServicesConfigTest {

  @Test
  void parseServicesParameters() {
    var node = jsonNodeFromResource("standalone/config/router-config.json");
    var c = new RouterConfig(node, this.getClass().getSimpleName(), false);
    assertEquals(
      List.of(
        new RideHailingServiceParameters(
          "secret-id",
          "very-secret",
          "car",
          List.of("type1", "type2", "type3")
        )
      ),
      c.rideHailingServiceParameters()
    );
  }
}
