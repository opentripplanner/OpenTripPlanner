package org.opentripplanner.apis.vectortiles.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.json.ObjectMappers;

/**
 * A style parameter that allows you to specify a number that changes dependent on the zoom level.
 */
public record ZoomDependentNumber(List<ZoomStop> stops) {
  private static final ObjectMapper OBJECT_MAPPER = ObjectMappers.ignoringExtraFields();
  public JsonNode toJson() {
    var interpolation = new ArrayList<>();
    interpolation.add("interpolate");
    interpolation.add(List.of("linear"));
    interpolation.add(List.of("zoom"));
    stops.forEach(s -> interpolation.addAll(s.toList()));

    return OBJECT_MAPPER.valueToTree(interpolation);
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
