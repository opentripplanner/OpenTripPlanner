package org.opentripplanner.apis.vectortiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestServerContext;
import org.opentripplanner.apis.vectortiles.model.LayerType;
import org.opentripplanner.ext.fares.impl.NoopFareServiceFactory;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.test.support.HttpForTest;
import org.opentripplanner.transit.service.TimetableRepository;

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

  @Test
  void tileJson() {
    var resource = new DebugVectorTilesResource(
      TestServerContext.createServerContext(
        new Graph(),
        new TimetableRepository(),
        new NoopFareServiceFactory().makeFareService()
      )
    );
    var req = HttpForTest.containerRequest();
    var tileJson = resource.getTileJson(req.getUriInfo(), req, "l1,l2");
    assertEquals(
      "https://localhost:8080/otp/debug/vectortiles/l1,l2/{z}/{x}/{y}.pbf",
      tileJson.tiles[0]
    );
  }

  @Test
  void tileJsonUrl() {
    var url = DebugVectorTilesResource.tileJsonUrl("http://example.com", LAYERS);
    assertEquals("http://example.com/otp/debug/vectortiles/edge,areaStop/tilejson.json", url);
  }
}
