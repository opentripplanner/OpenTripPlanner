package org.opentripplanner.api.common;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.api.resource.PlannerResource;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.configure.ConstructApplicationModule;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

class PlannerResourceTest {

  @Test
  void defaultValues() {
    var transitModel = new TransitModel();
    transitModel.initTimeZone(ZoneIds.BERLIN);
    var transitService = new DefaultTransitService(transitModel);
    var module = new ConstructApplicationModule();
    var context = module.providesServerContext(
      RouterConfig.DEFAULT,
      RaptorConfig.defaultConfigForTest(),
      new Graph(),
      transitService,
      null,
      null
    );
    var resource = new PlannerResource();

    resource.serverContext = context;

    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>(new HashMap<>());
    var req = resource.buildRequest(queryParams);
    assertNotNull(req);
    assertEquals(RequestModes.defaultRequestModes(), req.journey().modes());
  }
}
