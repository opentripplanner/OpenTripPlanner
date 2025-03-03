package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.agency;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.route;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequestBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;

class TripTimeOnDateMatcherFactoryTest {

  private static final RegularStop STOP = TimetableRepositoryForTest.of().stop("1").build();
  private static final LocalDate DATE = LocalDate.of(2025, 3, 2);

  private static final Route ROUTE_1 = route("r1")
    .withAgency(agency("a1"))
    .withMode(TransitMode.RAIL)
    .build();
  private static final Route ROUTE_2 = route("r2")
    .withAgency(agency("a2"))
    .withMode(TransitMode.BUS)
    .build();
  private static final Route ROUTE_3 = route("r2")
    .withAgency(agency("a3"))
    .withMode(TransitMode.FERRY)
    .build();

  @Test
  void noFilters() {
    var request = request().build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
  }

  @Test
  void includeRoute() {
    var request = request()
      .withIncludedRoutes(FilterValues.ofEmptyIsEverything("routes", List.of(ROUTE_1.getId())))
      .build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void excludeRoute() {
    var request = request()
      .withExcludedRoutes(FilterValues.ofEmptyIsEverything("routes", List.of(ROUTE_1.getId())))
      .build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void includeAgency() {
    var request = request()
      .withIncludedAgencies(
        FilterValues.ofEmptyIsEverything("agencies", List.of(ROUTE_1.getAgency().getId()))
      )
      .build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void excludeAgency() {
    var request = request()
      .withExcludedAgencies(
        FilterValues.ofEmptyIsEverything("agencies", List.of(ROUTE_1.getAgency().getId()))
      )
      .build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void includeMode() {
    var request = request()
      .withIncludedModes(FilterValues.ofEmptyIsEverything("mode", List.of(ROUTE_1.getMode())))
      .build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void excludeMode() {
    var request = request()
      .withExcludedModes(FilterValues.ofEmptyIsEverything("mode", List.of(ROUTE_1.getMode())))
      .build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertTrue(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void excludeModeAndRoute() {
    var request = request()
      .withExcludedModes(FilterValues.ofEmptyIsEverything("mode", List.of(ROUTE_1.getMode())))
      .withExcludedRoutes(FilterValues.ofEmptyIsEverything("routes", List.of(ROUTE_2.getId())))
      .build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
  }

  @Test
  void excludeAgencyAndRoute() {
    var request = request()
      .withExcludedModes(FilterValues.ofEmptyIsEverything("mode", List.of(ROUTE_1.getMode())))
      .withExcludedAgencies(
        FilterValues.ofEmptyIsEverything("agencies", List.of(ROUTE_2.getAgency().getId()))
      )
      .build();

    var matcher = TripTimeOnDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripTimeOnDate(ROUTE_1)));
    assertFalse(matcher.match(tripTimeOnDate(ROUTE_2)));
    assertTrue(matcher.match(tripTimeOnDate(ROUTE_3)));
  }

  private static TripTimeOnDateRequestBuilder request() {
    return TripTimeOnDateRequest.of(List.of(STOP)).withTime(Instant.EPOCH);
  }

  private static TripPattern pattern(Route route) {
    return TimetableRepositoryForTest
      .tripPattern("p1", route)
      .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP, STOP))
      .build();
  }

  private static TripTimeOnDate tripTimeOnDate(Route route1) {
    final TripPattern pattern = pattern(route1);
    var tripTimes = ScheduledTripTimes
      .of()
      .withTrip(TimetableRepositoryForTest.trip("t1").withRoute(route1).build())
      .withArrivalTimes("10:00 10:05")
      .withDepartureTimes("10:00 10:05")
      .build();
    return new TripTimeOnDate(tripTimes, 0, pattern, DATE, Instant.EPOCH);
  }
}
