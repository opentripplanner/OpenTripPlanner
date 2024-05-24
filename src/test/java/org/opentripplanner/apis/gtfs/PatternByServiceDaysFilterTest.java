package org.opentripplanner.apis.gtfs;

import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;

class PatternByServiceDaysFilterTest {

  private static final TransitService EMPTY_SERVICE = new DefaultTransitService(new TransitModel());

  static List<Arguments> invalidRange() {
    return List.of(
      Arguments.of(parse("2024-05-02"), parse("2024-05-01")),
      Arguments.of(parse("2024-05-03"), parse("2024-05-01"))
    );
  }

  @ParameterizedTest
  @MethodSource("invalidRange")
  void invalidRange(LocalDate start, LocalDate end) {
    assertThrows(
      IllegalArgumentException.class,
      () -> new PatternByServiceDaysFilter(EMPTY_SERVICE, start, end)
    );
  }

  static List<Arguments> validRange() {
    return List.of(
      Arguments.of(parse("2024-05-02"), parse("2024-05-02")),
      Arguments.of(parse("2024-05-02"), parse("2024-05-03")),
      Arguments.of(null, parse("2024-05-03")),
      Arguments.of(parse("2024-05-03"), null),
      Arguments.of(null, null)
    );
  }

  @ParameterizedTest
  @MethodSource("validRange")
  void validRange(LocalDate start, LocalDate end) {
    assertDoesNotThrow(() -> new PatternByServiceDaysFilter(EMPTY_SERVICE, start, end));
  }
}
