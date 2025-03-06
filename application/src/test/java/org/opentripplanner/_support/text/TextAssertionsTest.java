package org.opentripplanner._support.text;

import static org.opentripplanner._support.text.TextAssertions.assertLinesEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TextAssertionsTest {

  @Test
  void testIgnoreWhiteSpace() {
    // Empty text
    assertLinesEquals("", "\n\n");

    // Text with white-space inserted
    assertLinesEquals(
      """
      A Test
      Line 2
      DOS\r\n
      line-shift
      """,
      """

        A Test \t
      \t

      \tLine 2
      DOS\rline-shift
      """
    );
  }

  @Test
  void testEndOfText() {
    var ex = Assertions.assertThrows(org.opentest4j.AssertionFailedError.class, () ->
      assertLinesEquals("A\n", "A\nExtra Line")
    );
    Assertions.assertTrue(
      ex.getMessage().contains("Expected(@end-of-text)"),
      "<" + ex.getMessage() + "> does not contain expected line."
    );
    Assertions.assertTrue(
      ex.getMessage().contains("Actual  (@line 1): <Extra Line>"),
      "<" + ex.getMessage() + "> does not contain actual line."
    );
  }
}
