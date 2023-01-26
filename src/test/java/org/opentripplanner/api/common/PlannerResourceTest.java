package org.opentripplanner.api.common;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.api.resource.PlannerResource;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.configure.ConstructApplicationModule;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

class PlannerResourceTest {

  static OtpServerRequestContext context() {
    var transitModel = new TransitModel();
    transitModel.initTimeZone(ZoneIds.BERLIN);
    var transitService = new DefaultTransitService(transitModel);
    var module = new ConstructApplicationModule();
    return module.providesServerContext(
      RouterConfig.DEFAULT,
      RaptorConfig.defaultConfigForTest(),
      new Graph(),
      transitService,
      null,
      null
    );
  }

  @Test
  void defaultValues() {
    var resource = new PlannerResource();
    resource.serverContext = context();

    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>(new HashMap<>());
    var req = resource.buildRequest(queryParams);
    assertNotNull(req);
    assertEquals(RequestModes.defaultRequestModes(), req.journey().modes());
  }

  @Test
  void bicycleRent() {
    var resource = new PlannerResource();

    resource.modes = new QualifiedModeSet("BICYCLE_RENT");
    resource.serverContext = context();

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
