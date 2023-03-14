package org.opentripplanner.framework.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMappers {

  /**
   * Returns a mapper that doesn't fail on unknown properties.
   */
  public static ObjectMapper ignoringExtraFields() {
    var mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }
}
