package org.opentripplanner.apis.vectortiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.json.ObjectMappers;

public record MapboxStyleJson(
  String name,
  Map<String, VectorTileSource> sources,
  List<JsonNode> layers
) {
  public record VectorTileSource(String type, String url) {}

  public static class LayerStyleBuilder {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMappers.ignoringExtraFields();
    private final Map<String, Object> props = new HashMap<>();
    private final Map<String, Object> paint = new HashMap<>();

    public static LayerStyleBuilder ofId(String id) {
      return new LayerStyleBuilder(id);
    }

    private LayerStyleBuilder(String id) {
      props.put("id", id);
    }

    /**
     * Which vector tile source this should apply to.
     */
    public LayerStyleBuilder source(String source) {
      props.put("source", source);
      return this;
    }

    public LayerStyleBuilder circleColor(String color) {
      paint.put("circle-color", color);
      return this;
    }

    public JsonNode build() {
      var copy = new HashMap<>(props);
      if(!paint.isEmpty()) {
        copy.put("paint", paint);
      }
      return OBJECT_MAPPER.valueToTree(copy);
    }

  }
}
