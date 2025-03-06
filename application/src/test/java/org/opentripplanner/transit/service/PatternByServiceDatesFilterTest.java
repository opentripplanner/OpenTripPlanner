package org.opentripplanner.transit.service;

import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.transit.model._data.PatternTestModel.ROUTE_1;
import static org.opentripplanner.transit.service.PatternByServiceDatesFilterTest.FilterExpectation.NOT_REMOVED;
import static org.opentripplanner.transit.service.PatternByServiceDatesFilterTest.FilterExpectation.REMOVED;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.apis.gtfs.model.LocalDateRange;
import org.opentripplanner.transit.model._data.PatternTestModel;
import org.opentripplanner.transit.model.network.TripPattern;

class PatternByServiceDatesFilterTest {

  private static final TripPattern PATTERN_1 = PatternTestModel.pattern();

  enum FilterExpectation {
    REMOVED,
    NOT_REMOVED,
  }

  static List<Arguments> invalidRangeCases() {
    return List.of(
      Arguments.of(null, null),
      Arguments.of(parse("2024-05-02"), parse("2024-05-01")),
      Arguments.of(parse("2024-05-03"), parse("2024-05-01"))
    );
  }

  @ParameterizedTest
  @MethodSource("invalidRangeCases")
  void invalidRange(LocalDate start, LocalDate end) {
    assertThrows(IllegalArgumentException.class, () ->
      new PatternByServiceDatesFilter(
        new LocalDateRange(start, end),
        r -> List.of(),
        d -> List.of()
      )
    );
  }

  static List<Arguments> validRangeCases() {
    return List.of(
      Arguments.of(parse("2024-05-02"), parse("2024-05-02")),
      Arguments.of(parse("2024-05-02"), parse("2024-05-03")),
      Arguments.of(null, parse("2024-05-03")),
      Arguments.of(parse("2024-05-03"), null)
    );
  }

  @ParameterizedTest
  @MethodSource("validRangeCases")
  void validRange(LocalDate start, LocalDate end) {
    assertDoesNotThrow(() ->
      new PatternByServiceDatesFilter(
        new LocalDateRange(start, end),
        r -> List.of(),
        d -> List.of()
      )
    );
  }

  static List<Arguments> ranges() {
    return List.of(
      Arguments.of(null, parse("2024-05-03"), NOT_REMOVED),
      Arguments.of(parse("2024-05-03"), null, NOT_REMOVED),
      Arguments.of(parse("2024-05-01"), null, NOT_REMOVED),
      Arguments.of(null, parse("2024-04-30"), REMOVED),
      Arguments.of(null, parse("2024-05-01"), REMOVED),
      Arguments.of(parse("2024-05-02"), parse("2024-05-02"), REMOVED),
      Arguments.of(parse("2024-05-02"), parse("2024-05-03"), REMOVED),
      Arguments.of(parse("2024-05-02"), parse("2024-06-01"), REMOVED),
      Arguments.of(parse("2025-01-01"), null, REMOVED),
      Arguments.of(parse("2025-01-01"), parse("2025-01-02"), REMOVED),
      Arguments.of(null, parse("2023-12-31"), REMOVED),
      Arguments.of(parse("2023-12-31"), parse("2024-04-30"), REMOVED)
    );
  }

  @ParameterizedTest
  @MethodSource("ranges")
  void filterPatterns(LocalDate start, LocalDate end, FilterExpectation expectation) {
    var filter = defaultFilter(start, end);

    var filterInput = List.of(PATTERN_1);
    var filterOutput = filter.filterPatterns(filterInput);

    if (expectation == NOT_REMOVED) {
      assertEquals(filterOutput, filterInput);
    } else {
      assertEquals(List.of(), filterOutput);
    }
  }

  @ParameterizedTest
  @MethodSource("ranges")
  void filterRoutes(LocalDate start, LocalDate end, FilterExpectation expectation) {
    var filter = defaultFilter(start, end);

    var filterInput = List.of(ROUTE_1);
    var filterOutput = filter.filterRoutes(filterInput);

    if (expectation == NOT_REMOVED) {
      assertEquals(filterOutput, filterInput);
    } else {
      assertEquals(List.of(), filterOutput);
    }
  }

  private static PatternByServiceDatesFilter defaultFilter(LocalDate start, LocalDate end) {
    return new PatternByServiceDatesFilter(
      new LocalDateRange(start, end),
      route -> List.of(PATTERN_1),
      trip -> List.of(parse("2024-05-01"), parse("2024-06-01"))
    );
  }
}
