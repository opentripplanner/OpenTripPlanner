package org.opentripplanner.utils.time;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class OffsetDateTimeParserTest {

  private static final String TIME_STRING = "2023-01-27T12:59:00+01:00";
  private static final OffsetDateTime TIME = OffsetDateTime.parse(TIME_STRING);

  static List<String> successfulCases() {
    return List.of(
      TIME_STRING,
      "2023-01-27T12:59:00.000+01:00",
      "2023-01-27T12:59:00+0100",
      "2023-01-27T12:59:00+01",
      "2023-01-27T11:59:00Z",
      "2023-01-27T11:59Z",
      "2023-01-27T06:59:00-05:00",
      "2023-01-27T06:59:00-0500"
    );
  }

  @ParameterizedTest
  @MethodSource("successfulCases")
  void parse(String input) throws ParseException {
    var res = OffsetDateTimeParser.parseLeniently(input);
    assertTrue(res.isEqual(TIME));
  }

  static List<String> failedCases() {
    return List.of("2023-01-27T11:59:00", "2023-01-27T11", "2023-01-27T11:00");
  }

  @ParameterizedTest
  @MethodSource("failedCases")
  void failed(String input) {
    Assertions.assertThrows(ParseException.class, () -> {
      OffsetDateTimeParser.parseLeniently(input);
    });
  }
}
