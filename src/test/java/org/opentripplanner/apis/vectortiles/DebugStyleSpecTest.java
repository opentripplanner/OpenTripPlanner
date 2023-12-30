package org.opentripplanner.apis.vectortiles;

import static org.opentripplanner.test.support.JsonAssertions.assertEqualJson;

import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.vectortiles.DebugStyleSpec.VectorSourceLayer;
import org.opentripplanner.apis.vectortiles.model.TileSource.VectorSource;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.test.support.ResourceLoader;

class DebugStyleSpecTest {

  private final ResourceLoader RES = ResourceLoader.of(this);

  @Test
  void spec() {
    var vectorSource = new VectorSource("vectorSource", "https://example.com");
    var regularStops = new VectorSourceLayer(vectorSource, "regularStops");
    var spec = DebugStyleSpec.build(vectorSource, regularStops);

    var json = ObjectMappers.ignoringExtraFields().valueToTree(spec);
    var expectation = RES.fileToString("style.json");
    assertEqualJson(expectation, json);
  }
}
