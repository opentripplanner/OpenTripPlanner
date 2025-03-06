package org.opentripplanner.test.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

public class TestTableParser {

  /**
   * Parse a text-table of argument and produce one test-case for each line.
   * <p>
   * Example:
   * <pre>
   * """
   *  # input      ||  result
   *  # a | b | c  || a+b+c | a*b*c
   *    1 | 2 | 3  ||   6   |   6    # Expect sum and product to be the same
   *    1 | 2 | 4  ||   7   |   8    # Product is larger
   * """
   * </pre>
   * <ul>
   *   <li>Supported types are boolean, int, double and strings</li>
   *   <li>Use '#' to add comments/header line.</li>
   *   <li>A double pipe({@code '||'}) is optional and is usually used to separate input from expected
   * result.</li>
   * </ul>
   */
  public static Stream<Arguments> of(String text) {
    return text
      .lines()
      .map(String::trim)
      .map(TestTableParser::stripComment)
      .filter(Predicate.not(String::isBlank))
      .map(line -> () -> splitLine(line));
  }

  private static Object[] splitLine(String line) {
    var args = line.split("\s*\\|\\|?\s*");
    var result = new Object[args.length];

    for (int i = 0; i < args.length; i++) {
      result[i] = toValue(args[i]);
    }
    return result;
  }

  private static String stripComment(String line) {
    int pos = line.indexOf("#");
    return pos < 0 ? line : (pos == 0 ? "" : line.substring(0, pos).trim());
  }

  private static Object toValue(String arg) {
    if (arg.matches("-?\\d+")) {
      return Integer.valueOf(arg);
    }
    if (arg.matches("-?\\d*\\.\\d+")) {
      return Double.valueOf(arg);
    }
    String lcArg = arg.toLowerCase(Locale.ROOT);
    if (lcArg.matches("t(rue)?|y(es)?|x")) {
      return Boolean.TRUE;
    }
    if (lcArg.matches("f(alse)?|n(o)?|-")) {
      return Boolean.FALSE;
    }
    return arg;
  }

  @Test
  void testParser() {
    assertEquals(List.of(List.of("a", "b")), toLists("a | b"));
    assertEquals(
      List.of(List.of(true, false, true, false, true, false, true, false, true, false)),
      toLists("true | false | T | F | y | n | YES | NO  | x | -")
    );
    assertEquals(List.of(List.of(1, 1.2)), toLists("1 | 1.2"));
    assertEquals(
      List.of(List.of(1, 7)),
      toLists(
        """
        # input | output
              1 |      7
        """
      )
    );
    assertEquals(
      List.of(List.of(1, 2, 3, 6, 6), List.of(1, 2, 4, 7, 8)),
      toLists(
        """
        # input      ||  result
        # a | b | c  || a+b+c | a*b*c
          1 | 2 | 3  ||   6   |   6    # Expect sum and product to be the same
          1 | 2 | 4  ||   7   |   8    # Product is larger
        """
      )
    );
  }

  private static List<List<Object>> toLists(String text) {
    return of(text).map(Arguments::get).map(Arrays::asList).toList();
  }
}
