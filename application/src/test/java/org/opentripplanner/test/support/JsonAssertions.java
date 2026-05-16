package org.opentripplanner.test.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.opentripplanner.standalone.config.framework.json.JsonSupport;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class JsonAssertions {

  private static final ObjectMapper MAPPER = new JsonMapper();

  /**
   * Take two JSON documents and reformat them before comparing {@code actual} with {@code expected}.
   */
  public static void assertEqualJson(String expected, String actual) {
    try {
      assertEqualJson(expected, MAPPER.readTree(actual));
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @see JsonAssertions#assertEqualJson(String, String)
   */
  public static void assertEqualJson(String expected, JsonNode actual) {
    try {
      var actualNode = MAPPER.readTree(actual.toString());
      var exp = MAPPER.readTree(expected);
      assertEquals(exp, actualNode, () ->
        "Expected '%s' but actual was '%s'".formatted(
          JsonSupport.prettyPrint(exp),
          JsonSupport.prettyPrint(actualNode)
        )
      );
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Check that two JSONs are equal.
   */
  public static boolean isEqualJson(String expected, JsonNode actual) {
    try {
      var actualNode = MAPPER.readTree(actual.toString());
      var exp = MAPPER.readTree(expected);
      return exp.equals(actualNode);
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }
  }
}
