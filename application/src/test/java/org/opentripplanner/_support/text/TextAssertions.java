package org.opentripplanner._support.text;

import org.junit.jupiter.api.Assertions;

/**
 * This class contains test assert methods not supported by the standard JUnit
 * framework.
 */
public final class TextAssertions {

  private static final String LINE_DELIMITERS = "(\n|\r|\r\n)";
  private static final int END_OF_TEXT = -111;

  /**

   * Assert to texts are equals line by line. Empty lines and white-space in the start and end of
   * a line is ignored.
   */
  public static void assertLinesEquals(String expected, String actual) {
    var expLines = expected.split(LINE_DELIMITERS);
    var actLines = actual.split(LINE_DELIMITERS);

    int i = -1;
    int j = -1;

    while (true) {
      i = next(expLines, i);
      j = next(actLines, j);

      if (i == END_OF_TEXT && j == END_OF_TEXT) {
        return;
      }

      var exp = getLine(expLines, i);
      var act = getLine(actLines, j);

      if (i == END_OF_TEXT || j == END_OF_TEXT || !exp.equals(act)) {
        Assertions.fail(
          "Expected%s: <%s>%n".formatted(lineText(i), exp) +
          "Actual  %s: <%s>%n".formatted(lineText(j), act)
        );
      }
    }
  }

  private static String lineText(int index) {
    return index < 0 ? "(@end-of-text)" : "(@line %d)".formatted(index);
  }

  private static String getLine(String[] lines, int i) {
    return i == END_OF_TEXT ? "" : lines[i].trim();
  }

  private static int next(String[] lines, int index) {
    ++index;
    while (index < lines.length) {
      if (!lines[index].isBlank()) {
        return index;
      }
      ++index;
    }
    return END_OF_TEXT;
  }
}
