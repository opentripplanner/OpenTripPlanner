package org.opentripplanner.apis.vectortiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.TestServerContext;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.test.support.HttpForTest;
import org.opentripplanner.transit.service.TimetableRepository;

class InspectorResourceTest {

  @Test
  void tileJson() {
    var resource = new InspectorResource(
      TestServerContext.createServerContext(new Graph(), new TimetableRepository())
    );
    var req = HttpForTest.containerRequest();
    var tileJson = resource.getTileJson(req.getUriInfo(), req, "l1,l2");
    assertEquals("https://localhost:8080/otp/inspector/l1,l2/{z}/{x}/{y}.pbf", tileJson.tiles[0]);
  }
}
