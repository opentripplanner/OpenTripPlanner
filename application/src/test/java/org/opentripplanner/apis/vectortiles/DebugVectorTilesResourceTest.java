package org.opentripplanner.apis.vectortiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeForTest;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.vectortiles.model.LayerType;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.standalone.api.TestServerContext;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.test.support.HttpForTest;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.service.TransitRepository;

class DebugVectorTilesResourceTest {

  private record LayerParams(String name, LayerType type) implements LayerParameters<LayerType> {
    @Override
    public String mapper() {
      return "type";
    }
  }

  private static final List<LayerParameters<LayerType>> LAYERS = List.of(
    new LayerParams("edge", LayerType.Edge),
    new LayerParams("areaStop", LayerType.AreaStop)
  );

  private static DebugVectorTilesResource resource(DebugUiConfig config) {
    var transferRepository = TransferServiceTestFactory.defaultTransferRepository();
    var transitService = TestServerContext.createTransitService(
      new TransitRepository(),
      new TimetableDataImport(),
      transferRepository
    );
    return new DebugVectorTilesResource(
      transitService,
      new Graph(),
      config,
      TestServerContext.createWorldEnvelopeService(),
      TestServerContext.createVehicleRentalService(),
      TestServerContext.createStreetDetailsService(),
      TransferServiceTestFactory.transferService(transferRepository)
    );
  }

  @Test
  void tileJson() {
    var req = HttpForTest.containerRequest();
    var tileJson = resource(DebugUiConfig.DEFAULT).getTileJson(req.getUriInfo(), req, "l1,l2");
    assertEquals(
      "https://localhost:8080/otp/debug/vectortiles/l1,l2/{z}/{x}/{y}.pbf",
      tileJson.tiles[0]
    );
  }

  @Test
  void tileJsonWithBasePath() {
    var config = new DebugUiConfig(
      jsonNodeForTest("{ 'vectorTilesBasePath': '/gateway/debug/otp/debug/vectortiles' }"),
      "TEST",
      true
    );
    var req = HttpForTest.containerRequest();
    var tileJson = resource(config).getTileJson(req.getUriInfo(), req, "l1,l2");
    assertEquals(
      "https://localhost:8080/gateway/debug/otp/debug/vectortiles/l1,l2/{z}/{x}/{y}.pbf",
      tileJson.tiles[0]
    );
  }

  @Test
  void tileJsonUrl() {
    var url = DebugVectorTilesResource.tileJsonUrl(
      "http://example.com",
      DebugUiConfig.DEFAULT.vectorTilesBasePath(),
      LAYERS
    );
    assertEquals("http://example.com/otp/debug/vectortiles/edge,areaStop/tilejson.json", url);
  }

  @Test
  void tileJsonUrlWithBasePath() {
    // leading and trailing slashes of the base path are optional
    for (var basePath : List.of("/gateway/tiles", "gateway/tiles/", "/gateway/tiles/")) {
      var url = DebugVectorTilesResource.tileJsonUrl("http://example.com", basePath, LAYERS);
      assertEquals("http://example.com/gateway/tiles/edge,areaStop/tilejson.json", url);
    }
  }
}
