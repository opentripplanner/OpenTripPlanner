package org.opentripplanner.standalone.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RequestTraceFilterTest {

  /** The longest string accepted the check - 512 characters long*/
  private static final String A_VERY_LONG_STRING = IntStream.range(200, 712)
    .mapToObj(Character::toString)
    .collect(Collectors.joining());
  private static final String A_TOO_LONG_STRING = A_VERY_LONG_STRING + "1";

  @SuppressWarnings("unused")
  private static final Stream<Arguments> headerCheckTestCases() {
    return Stream.of(
      Arguments.of(true, "ok"),
      Arguments.of(true, "special characters: -_,;.:!#$%&/(){}[]=?+"),
      Arguments.of(true, "quote: \"quoted\" 'single' `back` ´forward´"),
      Arguments.of(true, "international characters: æøå öâò≈∰🧐"),
      Arguments.of(true, A_VERY_LONG_STRING),
      Arguments.of(false, A_TOO_LONG_STRING),
      Arguments.of(false, "Vertical space new-line: -\n-"),
      Arguments.of(false, "Vertical space return: -\r-"),
      Arguments.of(false, "Vertical space form-feed: -\f-"),
      Arguments.of(false, "Control character 0x01: -\u0001-"),
      Arguments.of(false, "Control character 0x19: -\u0019-")
    );
  }

  @ParameterizedTest
  @MethodSource("headerCheckTestCases")
  void headerCheck(boolean expectedMatch, String input) {
    assertEquals(
      expectedMatch,
      RequestTraceFilter.HTTP_HEADER_VALUE_CHECK.matcher(input).matches()
    );
  }
}
