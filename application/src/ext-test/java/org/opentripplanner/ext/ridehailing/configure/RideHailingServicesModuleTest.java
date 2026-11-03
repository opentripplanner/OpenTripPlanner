package org.opentripplanner.ext.ridehailing.configure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.ridehailing.service.uber.UberService;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.standalone.config.RouterConfig;
import tools.jackson.core.JacksonException;

class RideHailingServicesModuleTest {

  @Test
  void buildServices() throws JacksonException {
    var module = new RideHailingServicesModule();
    var json = """
    {
        "rideHailingServices": [
          {
            "type": "uber-car-hailing",
            "clientId": "secret-id",
            "clientSecret": "very-secret",
            "wheelchairAccessibleProductId": "product-id-accessible-by-wheelchair"
          }
        ]
    }
    """;
    var jsonNode = ObjectMappers.ignoringExtraFields().readTree(json);
    var services = module.services(new RouterConfig(jsonNode, json, false));

    assertEquals(1, services.size());

    var uberService = services.get(0);
    assertInstanceOf(UberService.class, uberService);
  }
}
