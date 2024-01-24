package org.opentripplanner.ext.restapi.resources;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestServerContextBuilder;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.transit.service.TransitModel;

class PlannerResourceTest {

  static TestServerContextBuilder context() {
    var transitModel = new TransitModel();
    transitModel.initTimeZone(ZoneIds.BERLIN);
    return TestServerContextBuilder.of().withTransitModel(transitModel);
  }

  @Test
  void defaultValues() {
    var context = context();
    var resource = new PlannerResource(context.serverContext(), context.transitService());

    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>(new HashMap<>());
    var req = resource.buildRequest(queryParams);
    assertNotNull(req);
    assertEquals(RequestModes.defaultRequestModes(), req.journey().modes());
  }

  @Test
  void bicycleRent() {
    var context = context();
    var resource = new PlannerResource(context.serverContext(), context.transitService());

    resource.modes = new QualifiedModeSet("BICYCLE_RENT");

    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>(new HashMap<>());
    var req = resource.buildRequest(queryParams);
    assertFalse(req.journey().transit().enabled());
    assertEquals(
      RequestModes
        .of()
        .withDirectMode(BIKE_RENTAL)
        .withEgressMode(BIKE_RENTAL)
        .withAccessMode(BIKE_RENTAL)
        .withTransferMode(BIKE_RENTAL)
        .build(),
      req.journey().modes()
    );
  }
}
