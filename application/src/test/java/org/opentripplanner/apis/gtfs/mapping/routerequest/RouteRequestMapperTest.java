package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static graphql.Assert.assertTrue;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class RouteRequestMapperTest {

  private static final Locale LOCALE = Locale.GERMAN;
  private final _RouteRequestTestContext testCtx = _RouteRequestTestContext.of(LOCALE);

  @Test
  void testMinimalArgs() {
    var env = testCtx.executionContext(testCtx.basicRequest());
    var defaultRequest = RouteRequest.defaultValue();
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());

    assertEquals(_RouteRequestTestContext.ORIGIN.x, routeRequest.from().lat);
    assertEquals(_RouteRequestTestContext.ORIGIN.y, routeRequest.from().lng);
    assertEquals(_RouteRequestTestContext.DESTINATION.x, routeRequest.to().lat);
    assertEquals(_RouteRequestTestContext.DESTINATION.y, routeRequest.to().lng);
    assertEquals(testCtx.locale(), routeRequest.preferences().locale());
    assertEquals(defaultRequest.journey().wheelchair(), routeRequest.journey().wheelchair());
    assertEquals(defaultRequest.arriveBy(), routeRequest.arriveBy());
    assertEquals(defaultRequest.numItineraries(), routeRequest.numItineraries());
    assertEquals(defaultRequest.searchWindow(), routeRequest.searchWindow());
    assertEquals(defaultRequest.journey().modes(), routeRequest.journey().modes());
    assertEquals(1, defaultRequest.journey().transit().filters().size());
    assertEquals(1, routeRequest.journey().transit().filters().size());
    assertTrue(routeRequest.journey().transit().enabled());
    assertEquals(
      defaultRequest.journey().transit().filters().toString(),
      routeRequest.journey().transit().filters().toString()
    );
    assertTrue(
      Duration.between(Instant.now(), routeRequest.dateTime()).compareTo(Duration.ofSeconds(10)) < 0
    );

    // Using current time as datetime changes rental availability use preferences, therefore to
    // check that the preferences are equal, we need to use a future time.
    var futureArgs = testCtx.basicRequest();
    var futureTime = OffsetDateTime.of(
      LocalDate.of(3000, 3, 15),
      LocalTime.MIDNIGHT,
      ZoneOffset.UTC
    );
    futureArgs.put("dateTime", Map.ofEntries(entry("earliestDeparture", futureTime)));
    var futureEnv = testCtx.executionContext(futureArgs);
    var futureRequest = RouteRequestMapper.toRouteRequest(futureEnv, testCtx.context());
    assertEquals(
      defaultRequest.preferences().copyOf().withLocale(LOCALE).build(),
      futureRequest.preferences()
    );
  }

  @Test
  void testEarliestDeparture() {
    var dateTimeArgs = testCtx.basicRequest();
    var dateTime = OffsetDateTime.of(LocalDate.of(2020, 3, 15), LocalTime.MIDNIGHT, ZoneOffset.UTC);
    dateTimeArgs.put("dateTime", Map.ofEntries(entry("earliestDeparture", dateTime)));
    var env = testCtx.executionContext(dateTimeArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertEquals(dateTime.toInstant(), routeRequest.dateTime());
    assertFalse(routeRequest.arriveBy());
    assertFalse(routeRequest.isAPIGtfsTripPlannedForNow(routeRequest.dateTime()));
  }

  @Test
  void testLatestArrival() {
    var dateTimeArgs = testCtx.basicRequest();
    var dateTime = OffsetDateTime.of(LocalDate.of(2020, 3, 15), LocalTime.MIDNIGHT, ZoneOffset.UTC);
    dateTimeArgs.put("dateTime", Map.ofEntries(entry("latestArrival", dateTime)));
    var env = testCtx.executionContext(dateTimeArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertEquals(dateTime.toInstant(), routeRequest.dateTime());
    assertTrue(routeRequest.arriveBy());
    assertFalse(routeRequest.isAPIGtfsTripPlannedForNow(routeRequest.dateTime()));
  }

  @Test
  void testRentalAvailability() {
    var nowArgs = testCtx.basicRequest();
    var nowEnv = testCtx.executionContext(nowArgs);
    var nowRequest = RouteRequestMapper.toRouteRequest(nowEnv, testCtx.context());
    assertTrue(nowRequest.preferences().bike().rental().useAvailabilityInformation());
    assertTrue(nowRequest.preferences().car().rental().useAvailabilityInformation());
    assertTrue(nowRequest.preferences().scooter().rental().useAvailabilityInformation());

    var futureArgs = testCtx.basicRequest();
    var futureTime = OffsetDateTime.of(
      LocalDate.of(3000, 3, 15),
      LocalTime.MIDNIGHT,
      ZoneOffset.UTC
    );
    futureArgs.put("dateTime", Map.ofEntries(entry("earliestDeparture", futureTime)));
    var futureEnv = testCtx.executionContext(futureArgs);
    var futureRequest = RouteRequestMapper.toRouteRequest(futureEnv, testCtx.context());
    assertFalse(futureRequest.preferences().bike().rental().useAvailabilityInformation());
    assertFalse(futureRequest.preferences().car().rental().useAvailabilityInformation());
    assertFalse(futureRequest.preferences().scooter().rental().useAvailabilityInformation());
  }

  @Test
  void testStopLocationAndLabel() {
    Map<String, Object> stopLocationArgs = new HashMap<>();
    var stopA = "foo:1";
    var stopB = "foo:2";
    var originLabel = "start";
    var destinationLabel = "end";
    stopLocationArgs.put(
      "origin",
      Map.ofEntries(
        entry("location", Map.of("stopLocation", Map.of("stopLocationId", stopA))),
        entry("label", originLabel)
      )
    );
    stopLocationArgs.put(
      "destination",
      Map.ofEntries(
        entry("location", Map.of("stopLocation", Map.of("stopLocationId", stopB))),
        entry("label", destinationLabel)
      )
    );
    var env = testCtx.executionContext(stopLocationArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertEquals(FeedScopedId.parse(stopA), routeRequest.from().stopId);
    assertEquals(originLabel, routeRequest.from().label);
    assertEquals(FeedScopedId.parse(stopB), routeRequest.to().stopId);
    assertEquals(destinationLabel, routeRequest.to().label);
  }

  @Test
  void testLocale() {
    var englishLocale = Locale.ENGLISH;
    var localeArgs = testCtx.basicRequest();
    localeArgs.put("locale", englishLocale);
    var env = testCtx.executionContext(localeArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertEquals(englishLocale, routeRequest.preferences().locale());
  }

  @Test
  void testSearchWindow() {
    var searchWindow = Duration.ofHours(5);
    var searchWindowArgs = testCtx.basicRequest();
    searchWindowArgs.put("searchWindow", searchWindow);
    var env = testCtx.executionContext(searchWindowArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertEquals(searchWindow, routeRequest.searchWindow());
  }

  @Test
  void testBefore() {
    var before = PageCursor.decode(
      "MXxQUkVWSU9VU19QQUdFfDIwMjQtMDMtMTVUMTM6MzU6MzlafHw0MG18U1RSRUVUX0FORF9BUlJJVkFMX1RJTUV8ZmFsc2V8MjAyNC0wMy0xNVQxNDoyODoxNFp8MjAyNC0wMy0xNVQxNToxNDoyMlp8MXw0MjUzfA=="
    );
    var last = 8;
    var beforeArgs = testCtx.basicRequest();
    beforeArgs.put("before", before.encode());
    beforeArgs.put("first", 3);
    beforeArgs.put("last", last);
    beforeArgs.put("numberOfItineraries", 3);
    var env = testCtx.executionContext(beforeArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertEquals(before, routeRequest.pageCursor());
    assertEquals(last, routeRequest.numItineraries());
  }

  @Test
  void testAfter() {
    var after = PageCursor.decode(
      "MXxORVhUX1BBR0V8MjAyNC0wMy0xNVQxNDo0MzoxNFp8fDQwbXxTVFJFRVRfQU5EX0FSUklWQUxfVElNRXxmYWxzZXwyMDI0LTAzLTE1VDE0OjI4OjE0WnwyMDI0LTAzLTE1VDE1OjE0OjIyWnwxfDQyNTN8"
    );
    var first = 8;
    var afterArgs = testCtx.basicRequest();
    afterArgs.put("after", after.encode());
    afterArgs.put("first", first);
    afterArgs.put("last", 3);
    afterArgs.put("numberOfItineraries", 3);
    var env = testCtx.executionContext(afterArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertEquals(after, routeRequest.pageCursor());
    assertEquals(first, routeRequest.numItineraries());
  }

  @Test
  void testNumberOfItinerariesForSearchWithoutPaging() {
    var itineraries = 8;
    var itinArgs = testCtx.basicRequest();
    itinArgs.put("first", itineraries);
    itinArgs.put("last", 3);
    var env = testCtx.executionContext(itinArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    assertEquals(itineraries, routeRequest.numItineraries());
  }

  @Test
  void testItineraryFilters() {
    var filterArgs = testCtx.basicRequest();
    var profile = ItineraryFilterDebugProfile.LIMIT_TO_NUM_OF_ITINERARIES;
    var keepOne = 0.4;
    var keepThree = 0.6;
    var multiplier = 3.5;
    filterArgs.put(
      "itineraryFilter",
      Map.ofEntries(
        entry("itineraryFilterDebugProfile", "LIMIT_TO_NUMBER_OF_ITINERARIES"),
        entry("groupSimilarityKeepOne", keepOne),
        entry("groupSimilarityKeepThree", keepThree),
        entry("groupedOtherThanSameLegsMaxCostMultiplier", multiplier)
      )
    );

    var env = testCtx.executionContext(filterArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, testCtx.context());
    var itinFilter = routeRequest.preferences().itineraryFilter();
    assertEquals(profile, itinFilter.debug());
    assertEquals(keepOne, itinFilter.groupSimilarityKeepOne());
    assertEquals(keepThree, itinFilter.groupSimilarityKeepThree());
    assertEquals(multiplier, itinFilter.groupedOtherThanSameLegsMaxCostMultiplier());
  }

  @Test
  void via() {
    Map<String, Object> arguments = testCtx.basicRequest();
    arguments.put(
      "via",
      List.of(
        Map.of("passThrough", Map.of("stopLocationIds", List.of("F:stop1"), "label", "a label"))
      )
    );

    var routeRequest = RouteRequestMapper.toRouteRequest(
      testCtx.executionContext(arguments),
      testCtx.context()
    );
    assertEquals(
      "[PassThroughViaLocation{label: a label, stopLocationIds: [F:stop1]}]",
      routeRequest.getViaLocations().toString()
    );

    var noParamsReq = RouteRequestMapper.toRouteRequest(
      testCtx.executionContext(testCtx.basicRequest()),
      testCtx.context()
    );
    assertEquals(List.of(), noParamsReq.getViaLocations());
  }
}
