package org.opentripplanner.apis.vectortiles;

import static org.opentripplanner.test.support.JsonAssertions.assertEqualJson;

import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.vectortiles.model.TileSource.VectorSource;
import org.opentripplanner.apis.vectortiles.model.VectorSourceLayer;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.test.support.ResourceLoader;

class DebugStyleSpecTest {

  private final ResourceLoader RESOURCES = ResourceLoader.of(this);

  @Test
  void spec() {
    var vectorSource = new VectorSource("vectorSource", "https://example.com");
    var regularStops = new VectorSourceLayer(vectorSource, "stops");
    var areaStops = new VectorSourceLayer(vectorSource, "stops");
    var groupStops = new VectorSourceLayer(vectorSource, "stops");
    var edges = new VectorSourceLayer(vectorSource, "edges");
    var vertices = new VectorSourceLayer(vectorSource, "vertices");
    var spec = DebugStyleSpec.build(regularStops, areaStops, groupStops, edges, vertices);

    var json = ObjectMappers.ignoringExtraFields().valueToTree(spec);
    var expectation = RESOURCES.fileToString("style.json");
    assertEqualJson(expectation, json);
  }
}
