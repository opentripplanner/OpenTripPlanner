package org.opentripplanner.test.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.standalone.config.framework.json.JsonSupport;

public class JsonAssertions {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Take two JSON documents and reformat them before comparing {@code actual} with {@code expected}.
   */
  public static void assertEqualJson(String expected, String actual) {
    try {
      assertEqualJson(expected, MAPPER.readTree(actual));
    } catch (JsonProcessingException e) {
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
      assertEquals(
        exp,
        actualNode,
        () ->
          "Expected '%s' but actual was '%s'".formatted(
              JsonSupport.prettyPrint(exp),
              JsonSupport.prettyPrint(actualNode)
            )
      );
    } catch (JsonProcessingException e) {
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
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
