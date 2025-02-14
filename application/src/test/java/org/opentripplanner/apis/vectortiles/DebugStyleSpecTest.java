package org.opentripplanner.apis.vectortiles;

import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.test.support.JsonAssertions.assertEqualJson;
import static org.opentripplanner.test.support.JsonAssertions.isEqualJson;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.vectortiles.model.TileSource.VectorSource;
import org.opentripplanner.apis.vectortiles.model.VectorSourceLayer;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.standalone.config.framework.json.JsonSupport;

class DebugStyleSpecTest {

  private static final File STYLE_FILE = new File(
    "src/test/resources/org/opentripplanner/apis/vectortiles/style.json"
  );

  /**
   * If style.json file is updated, the first run will fail.
   */
  @Test
  void spec() {
    var vectorSource = new VectorSource("vectorSource", "https://example.com");
    var regularStops = new VectorSourceLayer(vectorSource, "stops");
    var areaStops = new VectorSourceLayer(vectorSource, "stops");
    var groupStops = new VectorSourceLayer(vectorSource, "stops");
    var edges = new VectorSourceLayer(vectorSource, "edges");
    var vertices = new VectorSourceLayer(vectorSource, "vertices");
    var spec = DebugStyleSpec.build(
      regularStops,
      areaStops,
      groupStops,
      edges,
      vertices,
      List.of()
    );

    var json = ObjectMappers.ignoringExtraFields().valueToTree(spec);
    var expectation = readFile(STYLE_FILE);
    var newJson = JsonSupport.prettyPrint(json);
    // Order of keys in a JSON object can randomly change so only write to file when necessary
    if (!isEqualJson(expectation, json)) {
      writeFile(STYLE_FILE, newJson);
    }
    assertEqualJson(expectation, newJson);
  }
}
