package org.opentripplanner.test.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.json.ObjectMappers;

/**
 * Helper class for generating URLs to geojson.io.
 */
public class GeoJsonIo {

  private static final ObjectMapper MAPPER = ObjectMappers.geoJson();

  public static String toUrl(Geometry geometry) {
    try {
      var geoJson = MAPPER.writeValueAsString(geometry);
      var encoded = URLEncoder.encode(geoJson, StandardCharsets.UTF_8);
      return "http://geojson.io/#data=data:application/json,%s".formatted(encoded);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
