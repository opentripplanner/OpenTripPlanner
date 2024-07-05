package org.opentripplanner.framework.json;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.framework.geometry.GeometryUtils;

public class ObjectMappers {

  /**
   * Returns a mapper that doesn't fail on unknown properties.
   */
  public static ObjectMapper ignoringExtraFields() {
    var mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  /**
   * Returns a mapper that can serialize JTS geometries into GeoJSON.
   */
  public static ObjectMapper geoJson() {
    return new ObjectMapper().registerModule(new JtsModule(GeometryUtils.getGeometryFactory()));
  }
}
