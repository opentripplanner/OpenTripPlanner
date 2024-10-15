package org.opentripplanner.apis.vectortiles.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import org.opentripplanner.framework.json.ObjectMappers;

/**
 * A style parameter that allows you to specify a number that changes dependent on the zoom level.
 */
public record ZoomDependentNumber(float base, List<ZoomStop> stops) {
  private static final ObjectMapper OBJECT_MAPPER = ObjectMappers.ignoringExtraFields();
  public JsonNode toJson() {
    var props = new LinkedHashMap<>();
    props.put("base", base);
    var vals = stops.stream().map(ZoomStop::toList).toList();
    props.put("stops", vals);
    return OBJECT_MAPPER.valueToTree(props);
  }

  /**
   * @param zoom The zoom level.
   * @param value What the value should be at the specified zoom.
   */
  public record ZoomStop(int zoom, float value) {
    public List<Number> toList() {
      return List.of(zoom, value);
    }
  }
}
