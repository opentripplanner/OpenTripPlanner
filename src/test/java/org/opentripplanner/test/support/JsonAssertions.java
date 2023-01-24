package org.opentripplanner.test.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonAssertions {

  private static final ObjectMapper mapper = new ObjectMapper();

  public static void assertEqualJson(String expected, String actual) {
    try {
      var act = mapper.readTree(actual);
      var exp = mapper.readTree(expected);
      assertEquals(act.toPrettyString(), exp.toPrettyString());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
