package org.opentripplanner.ext.vectortiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.glassfish.grizzly.http.server.Request;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentripplanner.TestServerContext;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.test.support.HttpForTest;
import org.opentripplanner.transit.service.TimetableRepository;

class VectorTilesResourceTest {

  @Test
  void tileJson() {
    // the Grizzly request is awful to instantiate, using Mockito
    var grizzlyRequest = Mockito.mock(Request.class);
    var resource = new VectorTilesResource(
      TestServerContext.createServerContext(
        new Graph(),
        new TimetableRepository(),
        new DefaultFareService()
      ),
      grizzlyRequest,
      "default"
    );
    var req = HttpForTest.containerRequest();
    var tileJson = resource.getTileJson(req.getUriInfo(), req, "layer1,layer2");
    assertEquals(
      "https://localhost:8080/otp/routers/default/vectorTiles/layer1,layer2/{z}/{x}/{y}.pbf",
      tileJson.tiles[0]
    );

    assertEquals(9, tileJson.minzoom);
    assertEquals(20, tileJson.maxzoom);
  }
}
