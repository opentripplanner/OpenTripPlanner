package org.opentripplanner.apis.vectortiles;

import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.test.support.JsonAssertions.assertEqualJson;
import static org.opentripplanner.test.support.JsonAssertions.isEqualJson;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.vectortiles.model.TileSource.VectorSource;
import org.opentripplanner.apis.vectortiles.model.VectorSourceLayer;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.standalone.config.framework.json.JsonSupport;
import org.opentripplanner.test.support.ResourceLoader;

class DebugStyleSpecTest {

  private final ResourceLoader RESOURCES = ResourceLoader.of(this);

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
    var geofencingZones = new VectorSourceLayer(vectorSource, "geofencingZones");
    var rental = new VectorSourceLayer(vectorSource, "rental");
    var spec = DebugStyleSpec.build(
      regularStops,
      areaStops,
      groupStops,
      edges,
      vertices,
      geofencingZones,
      rental,
      List.of()
    );

    var json = ObjectMappers.ignoringExtraFields().valueToTree(spec);
    var file = RESOURCES.testResourceFile("style.json");
    var expectation = readFile(file);
    var newJson = JsonSupport.prettyPrint(json);
    // Order of keys in a JSON object can randomly change so only write to file when necessary
    if (!isEqualJson(expectation, json)) {
      writeFile(file, newJson);
    }
    assertEqualJson(expectation, newJson);
  }
}
