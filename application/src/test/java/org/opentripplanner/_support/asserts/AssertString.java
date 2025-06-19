package org.opentripplanner._support.asserts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AssertString {

  public static void assertEqualsIgnoreWhitespace(String expected, String value) {
    assertEquals(removeWhitespace(expected), removeWhitespace(value));
  }

  private static String removeWhitespace(String value) {
    return value.replaceAll("[\n\s]+", "");
  }

  @Test
  void testAssertEqualsIgnoreWhitespace() {
    assertEqualsIgnoreWhitespace(
      """
      multi
       line
      string
      """,
      "multi line string"
    );
  }
}
