package org.opentripplanner.ext.vectortiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.glassfish.grizzly.http.server.Request;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.standalone.api.TestServerContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.test.support.HttpForTest;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.service.TransitRepository;

class VectorTilesResourceTest {

  @Test
  void tileJson() {
    // the Grizzly request is awful to instantiate, using Mockito
    var grizzlyRequest = Mockito.mock(Request.class);
    var transitService = TestServerContext.createTransitService(
      new TransitRepository(),
      TransferServiceTestFactory.defaultTransferRepository()
    );
    var resource = new VectorTilesResource(
      transitService,
      RouterConfig.DEFAULT.vectorTileConfig(),
      TestServerContext.createWorldEnvelopeService(),
      TestServerContext.createVehicleRentalService(),
      TestServerContext.createVehicleParkingService(),
      new TransitAlertServiceImpl(),
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
