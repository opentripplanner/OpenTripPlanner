package org.opentripplanner.apis.vectortiles;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.test.support.JsonAssertions.assertEqualJson;
import static org.opentripplanner.test.support.JsonAssertions.isEqualJson;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.vectortiles.model.StyleSpec;
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
    var json = ObjectMappers.ignoringExtraFields().valueToTree(buildSpec());
    var file = RESOURCES.testResourceFile("style.json");
    var expectation = readFile(file);
    var newJson = JsonSupport.prettyPrint(json);
    // Order of keys in a JSON object can randomly change so only write to file when necessary
    if (!isEqualJson(expectation, json)) {
      writeFile(file, newJson);
    }
    assertEqualJson(expectation, newJson);
  }

  /**
   * The debug client narrows the rental layers to a set of networks by combining its own filter
   * with the one the server sent. MapLibre does not allow the legacy filter syntax and the
   * expression syntax to be mixed inside one filter, so the client can only do that as long as
   * every layer here speaks the same dialect.
   */
  @Test
  void allFiltersUseExpressionSyntax() {
    var json = ObjectMappers.ignoringExtraFields().valueToTree(buildSpec());

    for (JsonNode layer : json.path("layers")) {
      var filter = layer.path("filter");
      if (!filter.isMissingNode()) {
        assertWithMessage("layer '%s' has a legacy filter: %s", layer.path("id").asText(), filter)
          .that(isLegacyFilter(filter))
          .isFalse();
      }
    }
  }

  /**
   * A legacy filter names the property directly and compares it to literals, as in
   * {@code ["in", "class", "StreetEdge"]}, so none of its arguments is itself an array. An
   * expression reaches the property through {@code ["get", …]} and therefore always nests at least
   * one array, whether the property is the left operand ({@code ["==", ["get", "isStairs"], true]})
   * or the right one ({@code ["in", "no-drop-off", ["string", ["get", "zoneType"]]]}).
   */
  private static boolean isLegacyFilter(JsonNode filter) {
    if (!filter.isArray() || filter.isEmpty()) {
      return false;
    }
    // "all", "any" and "none" combine sub-filters in both dialects, so a legacy one can hide inside
    var operator = filter.get(0).asText();
    if (operator.equals("all") || operator.equals("any") || operator.equals("none")) {
      for (int i = 1; i < filter.size(); i++) {
        if (isLegacyFilter(filter.get(i))) {
          return true;
        }
      }
      return false;
    }
    for (int i = 1; i < filter.size(); i++) {
      if (filter.get(i).isArray()) {
        return false;
      }
    }
    return true;
  }

  private static StyleSpec buildSpec() {
    var vectorSource = new VectorSource("vectorSource", "https://example.com");
    var regularStops = new VectorSourceLayer(vectorSource, "stops");
    var areaStops = new VectorSourceLayer(vectorSource, "stops");
    var groupStops = new VectorSourceLayer(vectorSource, "stops");
    var edges = new VectorSourceLayer(vectorSource, "edges");
    var vertices = new VectorSourceLayer(vectorSource, "vertices");
    var geofencingZones = new VectorSourceLayer(vectorSource, "geofencingZones");
    var rental = new VectorSourceLayer(vectorSource, "rental");
    var transfers = new VectorSourceLayer(vectorSource, "transfers");
    return DebugStyleSpec.build(
      regularStops,
      areaStops,
      groupStops,
      edges,
      vertices,
      geofencingZones,
      rental,
      transfers,
      List.of(),
      List.of("tier", "voi")
    );
  }
}
