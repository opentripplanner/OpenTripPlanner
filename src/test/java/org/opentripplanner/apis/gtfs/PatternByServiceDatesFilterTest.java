package org.opentripplanner.apis.gtfs;

import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.apis.gtfs.PatternByServiceDatesFilterTest.FilterExpectation.NOT_REMOVED;
import static org.opentripplanner.apis.gtfs.PatternByServiceDatesFilterTest.FilterExpectation.REMOVED;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLServiceDateFilterInput;
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

class PatternByServiceDatesFilterTest {

  private static final TransitService EMPTY_SERVICE = new DefaultTransitService(new TransitModel());
  private static final Route ROUTE_1 = TransitModelForTest.route("1").build();
  private static final Trip TRIP = TransitModelForTest.trip("t1").withRoute(ROUTE_1).build();
  private static final TransitModelForTest MODEL = new TransitModelForTest(StopModel.of());
  private static final RegularStop STOP_1 = MODEL.stop("1").build();
  private static final StopPattern STOP_PATTERN = TransitModelForTest.stopPattern(STOP_1, STOP_1);
  private static final TripPattern PATTERN_1 = pattern();
  private static final LocalDate DATE = LocalDate.parse("2024-05-27");

  enum FilterExpectation {
    REMOVED,
    NOT_REMOVED,
  }

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
      () -> new PatternByServiceDatesFilter(EMPTY_SERVICE, start, end)
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
    assertDoesNotThrow(() -> new PatternByServiceDatesFilter(EMPTY_SERVICE, start, end));
  }

  static List<Arguments> ranges() {
    return List.of(
      Arguments.of(null, null, NOT_REMOVED),
      Arguments.of(null, parse("2024-05-03"), NOT_REMOVED),
      Arguments.of(null, parse("2024-05-01"), NOT_REMOVED),
      Arguments.of(parse("2024-05-03"), null, NOT_REMOVED),
      Arguments.of(parse("2024-05-01"), null, NOT_REMOVED),
      Arguments.of(parse("2024-05-02"), parse("2024-05-02"), REMOVED),
      Arguments.of(parse("2024-05-02"), parse("2024-05-03"), REMOVED),
      Arguments.of(parse("2025-01-01"), null, REMOVED),
      Arguments.of(parse("2025-01-01"), parse("2025-01-02"), REMOVED),
      Arguments.of(null, parse("2023-12-31"), REMOVED),
      Arguments.of(parse("2023-12-31"), parse("2024-04-30"), REMOVED)
    );
  }

  @ParameterizedTest
  @MethodSource("ranges")
  void filterPatterns(LocalDate start, LocalDate end, FilterExpectation expectation) {
    var service = mockService();
    var filter = new PatternByServiceDatesFilter(service, start, end);

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
    var service = mockService();
    var filter = new PatternByServiceDatesFilter(service, start, end);

    var filterInput = List.of(ROUTE_1);
    var filterOutput = filter.filterRoutes(filterInput.stream());

    if (expectation == NOT_REMOVED) {
      assertEquals(filterOutput, filterInput);
    } else {
      assertEquals(List.of(), filterOutput);
    }
  }

  private static TransitService mockService() {
    return new DefaultTransitService(new TransitModel()) {
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

  public static List<GraphQLServiceDateFilterInput> noFilterCases() {
    var list = new ArrayList<GraphQLServiceDateFilterInput>();
    list.add(null);
    list.add(new GraphQLServiceDateFilterInput(Map.of()));
    return list;
  }

  @ParameterizedTest
  @MethodSource("noFilterCases")
  void hasNoServiceDateFilter(GraphQLServiceDateFilterInput input) {
    assertFalse(PatternByServiceDatesFilter.hasServiceDateFilter(input));
  }

  public static List<Map<String, Object>> hasFilterCases() {
    return List.of(Map.of("start", DATE), Map.of("end", DATE), Map.of("start", DATE, "end", DATE));
  }

  @ParameterizedTest
  @MethodSource("hasFilterCases")
  void hasServiceDateFilter(Map<String, Object> params) {
    var input = new GraphQLServiceDateFilterInput(params);
    assertTrue(PatternByServiceDatesFilter.hasServiceDateFilter(input));
  }
}
