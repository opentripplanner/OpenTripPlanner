package org.opentripplanner.apis.vectortiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.vectortiles.DebugVectorTilesResource.tilesBaseUrl;
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
  void tileJsonWithFullUrl() {
    var config = new DebugUiConfig(
      jsonNodeForTest(
        "{ 'vectorTilesBasePath': 'https://api.example.com/gateway/debug/otp/debug/vectortiles/' }"
      ),
      "TEST",
      true
    );
    var req = HttpForTest.containerRequest();
    var tileJson = resource(config).getTileJson(req.getUriInfo(), req, "l1,l2");
    // the host of the request is not used
    assertEquals(
      "https://api.example.com/gateway/debug/otp/debug/vectortiles/l1,l2/{z}/{x}/{y}.pbf",
      tileJson.tiles[0]
    );
  }

  @Test
  void baseUrlFromPathOrUrl() {
    var host = "http://example.com";
    var path = DebugUiConfig.DEFAULT.vectorTilesBasePath();
    assertEquals("http://example.com/otp/debug/vectortiles", tilesBaseUrl(host, path));
    // leading and trailing slashes of a path are optional
    for (var basePath : List.of("/gateway/tiles", "gateway/tiles/", "/gateway/tiles/")) {
      assertEquals("http://example.com/gateway/tiles", tilesBaseUrl(host, basePath));
    }
    // a full URL is used as is, apart from a trailing slash
    assertEquals(
      "https://api.example.com/tiles",
      tilesBaseUrl(host, "https://api.example.com/tiles/")
    );
    assertEquals(
      "HTTP://api.example.com/tiles",
      tilesBaseUrl(host, "HTTP://api.example.com/tiles")
    );
  }

  @Test
  void tileJsonUrl() {
    var url = DebugVectorTilesResource.tileJsonUrl("http://example.com/tiles", LAYERS);
    assertEquals("http://example.com/tiles/edge,areaStop/tilejson.json", url);
  }
}
