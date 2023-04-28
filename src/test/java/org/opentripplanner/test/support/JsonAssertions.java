package org.opentripplanner.test.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.standalone.config.framework.json.JsonSupport;

public class JsonAssertions {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Take two JSON documents and reformat them before comparing {@code actual} with {@code expected}.
   */
  public static void assertEqualJson(String expected, String actual) {
    try {
      var act = MAPPER.readTree(actual);
      var exp = MAPPER.readTree(expected);
      assertEquals(JsonSupport.prettyPrint(exp), JsonSupport.prettyPrint(act));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
