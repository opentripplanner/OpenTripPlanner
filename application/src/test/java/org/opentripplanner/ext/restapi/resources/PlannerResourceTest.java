package org.opentripplanner.ext.restapi.resources;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestServerContext;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TimetableRepository;

class PlannerResourceTest {

  static OtpServerRequestContext context() {
    var timetableRepository = new TimetableRepository();
    timetableRepository.initTimeZone(ZoneIds.BERLIN);
    return TestServerContext.createServerContext(new Graph(), timetableRepository);
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
      RequestModes.of()
        .withDirectMode(BIKE_RENTAL)
        .withEgressMode(BIKE_RENTAL)
        .withAccessMode(BIKE_RENTAL)
        .withTransferMode(BIKE_RENTAL)
        .build(),
      req.journey().modes()
    );
  }
}
