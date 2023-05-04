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
          "545de0c4-659f-49c6-be65-0d5e448dffd5",
          List.of("1196d0dd-423b-4a81-a1d8-615367d3a365", "f58761e5-8dd5-4940-a472-872f1236c596")
        )
      ),
      c.rideHailingServiceParameters()
    );
  }
}
