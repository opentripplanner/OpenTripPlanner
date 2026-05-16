package org.opentripplanner.framework.json;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import org.opentripplanner.street.geometry.GeometryUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class ObjectMappers {

  /**
   * Returns a mapper that doesn't fail on unknown properties.
   */
  public static ObjectMapper ignoringExtraFields() {
    var mapper = new JsonMapper();
    return mapper;
  }

  /**
   * Returns a mapper that can serialize JTS geometries into GeoJSON.
   */
  public static ObjectMapper geoJson() {
    return JsonMapper.builder()
      .addModule(new JtsModule(GeometryUtils.getGeometryFactory()))
      .build();
  }
}
