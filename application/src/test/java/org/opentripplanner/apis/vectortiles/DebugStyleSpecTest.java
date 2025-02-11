package org.opentripplanner.apis.vectortiles;

import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;

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
    writeFile(STYLE_FILE, JsonSupport.prettyPrint(json));
    assertFileEquals(expectation, STYLE_FILE);
  }
}
