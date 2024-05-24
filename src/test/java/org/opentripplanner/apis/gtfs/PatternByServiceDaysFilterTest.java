package org.opentripplanner.apis.gtfs;

import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;

class PatternByServiceDaysFilterTest {

  private static final TransitService EMPTY_SERVICE = new DefaultTransitService(new TransitModel());
  private static final Route ROUTE_1 = TransitModelForTest.route("1").build();
  private static final Trip TRIP = TransitModelForTest.trip("t1").withRoute(ROUTE_1).build();
  private static final TransitModelForTest MODEL = new TransitModelForTest(StopModel.of());
  private static final RegularStop STOP_1 = MODEL.stop("1").build();
  private static final StopPattern STOP_PATTERN = TransitModelForTest.stopPattern(STOP_1, STOP_1);
  private static final TripPattern PATTERN_1 = pattern();
  private static final List<TripPattern> NOT_REMOVED = List.of(PATTERN_1);
  private static final List<Object> REMOVED = List.of();

  private static TripPattern pattern() {
    var pattern = TransitModelForTest
      .tripPattern("1", ROUTE_1)
      .withStopPattern(STOP_PATTERN)
      .build();

    var tt = ScheduledTripTimes
      .of()
      .withTrip(TRIP)
      .withArrivalTimes("10:00 10:05")
      .withDepartureTimes("10:00 10:05")
      .build();
    pattern.add(tt);
    return pattern;
  }

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

  static List<Arguments> patternRanges() {
    return List.of(
      Arguments.of(null, null, NOT_REMOVED),
      Arguments.of(null, parse("2024-05-03"), NOT_REMOVED),
      Arguments.of(null, parse("2024-05-01"), NOT_REMOVED),
      Arguments.of(parse("2024-05-03"), null, NOT_REMOVED),
      Arguments.of(parse("2024-05-02"), parse("2024-05-02"), REMOVED),
      Arguments.of(parse("2024-05-02"), parse("2024-05-03"), REMOVED),
      Arguments.of(parse("2025-01-01"), null, REMOVED),
      Arguments.of(parse("2025-01-01"), parse("2025-01-02"), REMOVED),
      Arguments.of(null, parse("2023-12-31"), REMOVED),
      Arguments.of(parse("2023-12-31"), parse("2024-04-30"), REMOVED)
    );
  }

  @ParameterizedTest
  @MethodSource("patternRanges")
  void filterPatterns(LocalDate start, LocalDate end, List<TripPattern> expectedPatterns) {
    var model = new TransitModel();
    var service = mockService(model);

    var filter = new PatternByServiceDaysFilter(service, start, end);

    var result = filter.filterPatterns(NOT_REMOVED);

    assertEquals(expectedPatterns, result);
  }

  private static TransitService mockService(TransitModel model) {
    return new DefaultTransitService(model) {
      @Override
      public Collection<TripPattern> getPatternsForRoute(Route route) {
        return Set.of(PATTERN_1);
      }

      @Override
      public CalendarService getCalendarService() {
        return new CalendarService() {
          @Override
          public Set<FeedScopedId> getServiceIds() {
            return Set.of();
          }

          @Override
          public Set<LocalDate> getServiceDatesForServiceId(FeedScopedId serviceId) {
            return Set.of(parse("2024-05-01"), parse("2024-06-01"));
          }

          @Override
          public Set<FeedScopedId> getServiceIdsOnDate(LocalDate date) {
            return Set.of();
          }
        };
      }
    };
  }
}
