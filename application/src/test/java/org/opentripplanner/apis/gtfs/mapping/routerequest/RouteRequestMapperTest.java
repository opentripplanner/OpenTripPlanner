package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static graphql.Assert.assertTrue;
import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.SchemaFactory;
import org.opentripplanner.apis.gtfs.TestRoutingService;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;

class RouteRequestMapperTest {

  private static final Coordinate ORIGIN = new Coordinate(1.0, 2.0);
  private static final Coordinate DESTINATION = new Coordinate(2.0, 1.0);
  private static final Locale LOCALE = Locale.GERMAN;
  static final GraphQLRequestContext CONTEXT;
  static final Map<String, Object> ARGS = Map.ofEntries(
    entry(
      "origin",
      Map.ofEntries(
        entry("location", Map.of("coordinate", Map.of("latitude", ORIGIN.x, "longitude", ORIGIN.y)))
      )
    ),
    entry(
      "destination",
      Map.ofEntries(
        entry(
          "location",
          Map.of("coordinate", Map.of("latitude", DESTINATION.x, "longitude", DESTINATION.y))
        )
      )
    )
  );

  static {
    Graph graph = new Graph();
    var timetableRepository = new TimetableRepository();
    timetableRepository.initTimeZone(ZoneIds.BERLIN);
    final DefaultTransitService transitService = new DefaultTransitService(timetableRepository);
    var routeRequest = new RouteRequest();
    CONTEXT = new GraphQLRequestContext(
      new TestRoutingService(List.of()),
      transitService,
      new DefaultFareService(),
      new DefaultVehicleRentalService(),
      new DefaultVehicleParkingService(new DefaultVehicleParkingRepository()),
      new DefaultRealtimeVehicleService(transitService),
      SchemaFactory.createSchemaWithDefaultInjection(routeRequest),
      GraphFinder.getInstance(graph, transitService::findRegularStopsByBoundingBox),
      routeRequest
    );
  }

  @Test
  void testMinimalArgs() {
    var env = executionContext(ARGS, LOCALE, CONTEXT);
    var defaultRequest = new RouteRequest();
    var routeRequest = RouteRequestMapper.toRouteRequest(env, CONTEXT);
    assertEquals(ORIGIN.x, routeRequest.from().lat);
    assertEquals(ORIGIN.y, routeRequest.from().lng);
    assertEquals(DESTINATION.x, routeRequest.to().lat);
    assertEquals(DESTINATION.y, routeRequest.to().lng);
    assertEquals(LOCALE, routeRequest.locale());
    assertEquals(defaultRequest.wheelchair(), routeRequest.wheelchair());
    assertEquals(defaultRequest.arriveBy(), routeRequest.arriveBy());
    assertEquals(defaultRequest.isTripPlannedForNow(), routeRequest.isTripPlannedForNow());
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
      Duration.between(defaultRequest.dateTime(), routeRequest.dateTime()).compareTo(
        Duration.ofSeconds(10)
      ) <
      0
    );

    // Using current time as datetime changes rental availability use preferences, therefore to
    // check that the preferences are equal, we need to use a future time.
    var futureArgs = createArgsCopy(ARGS);
    var futureTime = OffsetDateTime.of(
      LocalDate.of(3000, 3, 15),
      LocalTime.MIDNIGHT,
      ZoneOffset.UTC
    );
    futureArgs.put("dateTime", Map.ofEntries(entry("earliestDeparture", futureTime)));
    var futureEnv = executionContext(futureArgs, LOCALE, CONTEXT);
    var futureRequest = RouteRequestMapper.toRouteRequest(futureEnv, CONTEXT);
    assertEquals(defaultRequest.preferences(), futureRequest.preferences());
  }

  @Test
  void testEarliestDeparture() {
    var dateTimeArgs = createArgsCopy(ARGS);
    var dateTime = OffsetDateTime.of(LocalDate.of(2020, 3, 15), LocalTime.MIDNIGHT, ZoneOffset.UTC);
    dateTimeArgs.put("dateTime", Map.ofEntries(entry("earliestDeparture", dateTime)));
    var env = executionContext(dateTimeArgs, LOCALE, CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, CONTEXT);
    assertEquals(dateTime.toInstant(), routeRequest.dateTime());
    assertFalse(routeRequest.arriveBy());
    assertFalse(routeRequest.isTripPlannedForNow());
  }

  @Test
  void testLatestArrival() {
    var dateTimeArgs = createArgsCopy(ARGS);
    var dateTime = OffsetDateTime.of(LocalDate.of(2020, 3, 15), LocalTime.MIDNIGHT, ZoneOffset.UTC);
    dateTimeArgs.put("dateTime", Map.ofEntries(entry("latestArrival", dateTime)));
    var env = executionContext(dateTimeArgs, LOCALE, CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, CONTEXT);
    assertEquals(dateTime.toInstant(), routeRequest.dateTime());
    assertTrue(routeRequest.arriveBy());
    assertFalse(routeRequest.isTripPlannedForNow());
  }

  @Test
  void testRentalAvailability() {
    var nowArgs = createArgsCopy(ARGS);
    var nowEnv = executionContext(nowArgs, LOCALE, CONTEXT);
    var nowRequest = RouteRequestMapper.toRouteRequest(nowEnv, CONTEXT);
    assertTrue(nowRequest.preferences().bike().rental().useAvailabilityInformation());
    assertTrue(nowRequest.preferences().car().rental().useAvailabilityInformation());
    assertTrue(nowRequest.preferences().scooter().rental().useAvailabilityInformation());

    var futureArgs = createArgsCopy(ARGS);
    var futureTime = OffsetDateTime.of(
      LocalDate.of(3000, 3, 15),
      LocalTime.MIDNIGHT,
      ZoneOffset.UTC
    );
    futureArgs.put("dateTime", Map.ofEntries(entry("earliestDeparture", futureTime)));
    var futureEnv = executionContext(futureArgs, LOCALE, CONTEXT);
    var futureRequest = RouteRequestMapper.toRouteRequest(futureEnv, CONTEXT);
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
    var env = executionContext(stopLocationArgs, LOCALE, CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, CONTEXT);
    assertEquals(FeedScopedId.parse(stopA), routeRequest.from().stopId);
    assertEquals(originLabel, routeRequest.from().label);
    assertEquals(FeedScopedId.parse(stopB), routeRequest.to().stopId);
    assertEquals(destinationLabel, routeRequest.to().label);
  }

  @Test
  void testLocale() {
    var englishLocale = Locale.ENGLISH;
    var localeArgs = createArgsCopy(ARGS);
    localeArgs.put("locale", englishLocale);
    var env = executionContext(localeArgs, LOCALE, CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, CONTEXT);
    assertEquals(englishLocale, routeRequest.locale());
  }

  @Test
  void testSearchWindow() {
    var searchWindow = Duration.ofHours(5);
    var searchWindowArgs = createArgsCopy(ARGS);
    searchWindowArgs.put("searchWindow", searchWindow);
    var env = executionContext(searchWindowArgs, LOCALE, CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, CONTEXT);
    assertEquals(searchWindow, routeRequest.searchWindow());
  }

  @Test
  void testBefore() {
    var before = PageCursor.decode(
      "MXxQUkVWSU9VU19QQUdFfDIwMjQtMDMtMTVUMTM6MzU6MzlafHw0MG18U1RSRUVUX0FORF9BUlJJVkFMX1RJTUV8ZmFsc2V8MjAyNC0wMy0xNVQxNDoyODoxNFp8MjAyNC0wMy0xNVQxNToxNDoyMlp8MXw0MjUzfA=="
    );
    var last = 8;
    var beforeArgs = createArgsCopy(ARGS);
    beforeArgs.put("before", before.encode());
    beforeArgs.put("first", 3);
    beforeArgs.put("last", last);
    beforeArgs.put("numberOfItineraries", 3);
    var env = executionContext(beforeArgs, LOCALE, CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, CONTEXT);
    assertEquals(before, routeRequest.pageCursor());
    assertEquals(last, routeRequest.numItineraries());
  }

  @Test
  void testAfter() {
    var after = PageCursor.decode(
      "MXxORVhUX1BBR0V8MjAyNC0wMy0xNVQxNDo0MzoxNFp8fDQwbXxTVFJFRVRfQU5EX0FSUklWQUxfVElNRXxmYWxzZXwyMDI0LTAzLTE1VDE0OjI4OjE0WnwyMDI0LTAzLTE1VDE1OjE0OjIyWnwxfDQyNTN8"
    );
    var first = 8;
    var afterArgs = createArgsCopy(ARGS);
    afterArgs.put("after", after.encode());
    afterArgs.put("first", first);
    afterArgs.put("last", 3);
    afterArgs.put("numberOfItineraries", 3);
    var env = executionContext(afterArgs, LOCALE, CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, CONTEXT);
    assertEquals(after, routeRequest.pageCursor());
    assertEquals(first, routeRequest.numItineraries());
  }

  @Test
  void testNumberOfItinerariesForSearchWithoutPaging() {
    var itineraries = 8;
    var itinArgs = createArgsCopy(ARGS);
    itinArgs.put("first", itineraries);
    itinArgs.put("last", 3);
    var env = executionContext(itinArgs, LOCALE, CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, CONTEXT);
    assertEquals(itineraries, routeRequest.numItineraries());
  }

  @Test
  void testItineraryFilters() {
    var filterArgs = createArgsCopy(ARGS);
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
    var env = executionContext(filterArgs, LOCALE, CONTEXT);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, CONTEXT);
    var itinFilter = routeRequest.preferences().itineraryFilter();
    assertEquals(profile, itinFilter.debug());
    assertEquals(keepOne, itinFilter.groupSimilarityKeepOne());
    assertEquals(keepThree, itinFilter.groupSimilarityKeepThree());
    assertEquals(multiplier, itinFilter.groupedOtherThanSameLegsMaxCostMultiplier());
  }

  @Test
  void via() {
    Map<String, Object> arguments = createArgsCopy(ARGS);
    arguments.put(
      "via",
      List.of(
        Map.of("passThrough", Map.of("stopLocationIds", List.of("F:stop1"), "label", "a label"))
      )
    );

    var routeRequest = RouteRequestMapper.toRouteRequest(
      executionContext(arguments, LOCALE, CONTEXT),
      CONTEXT
    );
    assertEquals(
      "[PassThroughViaLocation{label: a label, stopLocationIds: [F:stop1]}]",
      routeRequest.getViaLocations().toString()
    );

    var noParamsReq = RouteRequestMapper.toRouteRequest(
      executionContext(ARGS, LOCALE, CONTEXT),
      CONTEXT
    );
    assertEquals(List.of(), noParamsReq.getViaLocations());
  }

  static Map<String, Object> createArgsCopy(Map<String, Object> arguments) {
    Map<String, Object> newArgs = new HashMap<>();
    newArgs.putAll(arguments);
    return newArgs;
  }

  static DataFetchingEnvironment executionContext(
    Map<String, Object> arguments,
    Locale locale,
    GraphQLRequestContext requestContext
  ) {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput()
      .query("")
      .operationName("planConnection")
      .context(requestContext)
      .locale(locale)
      .build();

    var executionContext = newExecutionContextBuilder()
      .executionInput(executionInput)
      .executionId(ExecutionId.from("planConnectionTest"))
      .build();
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .arguments(arguments)
      .localContext(Map.of("locale", locale))
      .build();
  }
}
