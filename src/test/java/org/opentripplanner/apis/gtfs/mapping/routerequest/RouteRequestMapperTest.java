package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static graphql.Assert.assertTrue;
import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.TestRoutingService;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

class RouteRequestMapperTest {

  private static final Coordinate ORIGIN = new Coordinate(1.0, 2.0);
  private static final Coordinate DESTINATION = new Coordinate(2.0, 1.0);
  private static final Locale LOCALE = Locale.GERMAN;
  private static final GraphQLRequestContext context;
  private static final Map<String, Object> args = new HashMap<>();

  static {
    args.put(
      "origin",
      Map.ofEntries(
        entry("location", Map.of("coordinate", Map.of("latitude", ORIGIN.x, "longitude", ORIGIN.y)))
      )
    );
    args.put(
      "destination",
      Map.ofEntries(
        entry(
          "location",
          Map.of("coordinate", Map.of("latitude", DESTINATION.x, "longitude", DESTINATION.y))
        )
      )
    );

    Graph graph = new Graph();
    var transitModel = new TransitModel();
    transitModel.initTimeZone(ZoneIds.BERLIN);
    final DefaultTransitService transitService = new DefaultTransitService(transitModel);
    context =
      new GraphQLRequestContext(
        new TestRoutingService(List.of()),
        transitService,
        new DefaultFareService(),
        graph.getVehicleParkingService(),
        new DefaultVehicleRentalService(),
        new DefaultRealtimeVehicleService(transitService),
        GraphFinder.getInstance(graph, transitService::findRegularStops),
        new RouteRequest()
      );
  }

  @Test
  void testMinimalArgs() {
    var env = executionContext(args);
    var defaultRequest = new RouteRequest();
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    assertEquals(ORIGIN.x, routeRequest.from().lat);
    assertEquals(ORIGIN.y, routeRequest.from().lng);
    assertEquals(DESTINATION.x, routeRequest.to().lat);
    assertEquals(DESTINATION.y, routeRequest.to().lng);
    assertEquals(LOCALE, routeRequest.locale());
    assertEquals(defaultRequest.preferences(), routeRequest.preferences());
    assertEquals(defaultRequest.wheelchair(), routeRequest.wheelchair());
    assertEquals(defaultRequest.arriveBy(), routeRequest.arriveBy());
    assertEquals(defaultRequest.isTripPlannedForNow(), routeRequest.isTripPlannedForNow());
    assertEquals(defaultRequest.numItineraries(), routeRequest.numItineraries());
    assertEquals(defaultRequest.searchWindow(), routeRequest.searchWindow());
    assertEquals(defaultRequest.journey().modes(), routeRequest.journey().modes());
    assertTrue(defaultRequest.journey().transit().filters().size() == 1);
    assertTrue(routeRequest.journey().transit().filters().size() == 1);
    assertTrue(routeRequest.journey().transit().enabled());
    assertEquals(
      defaultRequest.journey().transit().filters().toString(),
      routeRequest.journey().transit().filters().toString()
    );
    assertTrue(
      Duration
        .between(defaultRequest.dateTime(), routeRequest.dateTime())
        .compareTo(Duration.ofSeconds(10)) <
      0
    );
  }

  @Test
  void testEarliestDeparture() {
    var dateTimeArgs = createArgsCopy();
    var dateTime = OffsetDateTime.of(LocalDate.of(2020, 3, 15), LocalTime.MIDNIGHT, ZoneOffset.UTC);
    dateTimeArgs.put("dateTime", Map.ofEntries(entry("earliestDeparture", dateTime)));
    var env = executionContext(dateTimeArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    assertEquals(dateTime.toInstant(), routeRequest.dateTime());
    assertFalse(routeRequest.arriveBy());
    assertFalse(routeRequest.isTripPlannedForNow());
  }

  @Test
  void testLatestArrival() {
    var dateTimeArgs = createArgsCopy();
    var dateTime = OffsetDateTime.of(LocalDate.of(2020, 3, 15), LocalTime.MIDNIGHT, ZoneOffset.UTC);
    dateTimeArgs.put("dateTime", Map.ofEntries(entry("latestArrival", dateTime)));
    var env = executionContext(dateTimeArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    assertEquals(dateTime.toInstant(), routeRequest.dateTime());
    assertTrue(routeRequest.arriveBy());
    assertFalse(routeRequest.isTripPlannedForNow());
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
    var env = executionContext(stopLocationArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    assertEquals(FeedScopedId.parse(stopA), routeRequest.from().stopId);
    assertEquals(originLabel, routeRequest.from().label);
    assertEquals(FeedScopedId.parse(stopB), routeRequest.to().stopId);
    assertEquals(destinationLabel, routeRequest.to().label);
  }

  @Test
  void testLocale() {
    var englishLocale = Locale.ENGLISH;
    var localeArgs = createArgsCopy();
    localeArgs.put("locale", englishLocale);
    var env = executionContext(localeArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    assertEquals(englishLocale, routeRequest.locale());
  }

  @Test
  void testSearchWindow() {
    var searchWindow = Duration.ofHours(5);
    var searchWindowArgs = createArgsCopy();
    searchWindowArgs.put("searchWindow", searchWindow);
    var env = executionContext(searchWindowArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    assertEquals(searchWindow, routeRequest.searchWindow());
  }

  @Test
  void testBefore() {
    var before = PageCursor.decode(
      "MXxQUkVWSU9VU19QQUdFfDIwMjQtMDMtMTVUMTM6MzU6MzlafHw0MG18U1RSRUVUX0FORF9BUlJJVkFMX1RJTUV8ZmFsc2V8MjAyNC0wMy0xNVQxNDoyODoxNFp8MjAyNC0wMy0xNVQxNToxNDoyMlp8MXw0MjUzfA=="
    );
    var last = 8;
    var beforeArgs = createArgsCopy();
    beforeArgs.put("before", before.encode());
    beforeArgs.put("first", 3);
    beforeArgs.put("last", last);
    beforeArgs.put("numberOfItineraries", 3);
    var env = executionContext(beforeArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    assertEquals(before, routeRequest.pageCursor());
    assertEquals(last, routeRequest.numItineraries());
  }

  @Test
  void testAfter() {
    var after = PageCursor.decode(
      "MXxORVhUX1BBR0V8MjAyNC0wMy0xNVQxNDo0MzoxNFp8fDQwbXxTVFJFRVRfQU5EX0FSUklWQUxfVElNRXxmYWxzZXwyMDI0LTAzLTE1VDE0OjI4OjE0WnwyMDI0LTAzLTE1VDE1OjE0OjIyWnwxfDQyNTN8"
    );
    var first = 8;
    var afterArgs = createArgsCopy();
    afterArgs.put("after", after.encode());
    afterArgs.put("first", first);
    afterArgs.put("last", 3);
    afterArgs.put("numberOfItineraries", 3);
    var env = executionContext(afterArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    assertEquals(after, routeRequest.pageCursor());
    assertEquals(first, routeRequest.numItineraries());
  }

  @Test
  void testNumberOfItineraries() {
    var itineraries = 8;
    var itinArgs = createArgsCopy();
    itinArgs.put("numberOfItineraries", itineraries);
    var env = executionContext(itinArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    assertEquals(itineraries, routeRequest.numItineraries());
  }

  @Test
  void testDirectOnly() {
    var modesArgs = createArgsCopy();
    modesArgs.put("modes", Map.ofEntries(entry("directOnly", true)));
    var env = executionContext(modesArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    assertFalse(routeRequest.journey().transit().enabled());
  }

  @Test
  void testTransitOnly() {
    var modesArgs = createArgsCopy();
    modesArgs.put("modes", Map.ofEntries(entry("transitOnly", true)));
    var env = executionContext(modesArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    assertEquals(StreetMode.NOT_SET, routeRequest.journey().direct().mode());
  }

  @Test
  void testStreetModes() {
    var modesArgs = createArgsCopy();
    var bicycle = List.of("BICYCLE");
    modesArgs.put(
      "modes",
      Map.ofEntries(
        entry("direct", List.of("CAR")),
        entry(
          "transit",
          Map.ofEntries(
            entry("access", bicycle),
            entry("egress", bicycle),
            entry("transfer", bicycle)
          )
        )
      )
    );
    var env = executionContext(modesArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    assertEquals(StreetMode.CAR, routeRequest.journey().direct().mode());
    assertEquals(StreetMode.BIKE, routeRequest.journey().access().mode());
    assertEquals(StreetMode.BIKE, routeRequest.journey().egress().mode());
    assertEquals(StreetMode.BIKE, routeRequest.journey().transfer().mode());
  }

  @Test
  void testTransitModes() {
    var modesArgs = createArgsCopy();
    var tramCost = 1.5;
    modesArgs.put(
      "modes",
      Map.ofEntries(
        entry(
          "transit",
          Map.ofEntries(
            entry(
              "transit",
              List.of(
                Map.ofEntries(
                  entry("mode", "TRAM"),
                  entry("cost", Map.ofEntries(entry("reluctance", tramCost)))
                ),
                Map.ofEntries(entry("mode", "FERRY"))
              )
            )
          )
        )
      )
    );
    var env = executionContext(modesArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var reluctanceForMode = routeRequest.preferences().transit().reluctanceForMode();
    assertEquals(tramCost, reluctanceForMode.get(TransitMode.TRAM));
    assertNull(reluctanceForMode.get(TransitMode.FERRY));
    assertEquals(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: [FERRY, TRAM]}]}]",
      routeRequest.journey().transit().filters().toString()
    );
  }

  @Test
  void testItineraryFilters() {
    var filterArgs = createArgsCopy();
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
    var env = executionContext(filterArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var itinFilter = routeRequest.preferences().itineraryFilter();
    assertEquals(profile, itinFilter.debug());
    assertEquals(keepOne, itinFilter.groupSimilarityKeepOne());
    assertEquals(keepThree, itinFilter.groupSimilarityKeepThree());
    assertEquals(multiplier, itinFilter.groupedOtherThanSameLegsMaxCostMultiplier());
  }

  @Test
  void testBasicBikePreferences() {
    var bicycleArgs = createArgsCopy();
    var reluctance = 7.5;
    var speed = 15d;
    var boardCost = Cost.costOfSeconds(50);
    bicycleArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(
                entry("reluctance", reluctance),
                entry("speed", speed),
                entry("boardCost", boardCost)
              )
            )
          )
        )
      )
    );
    var env = executionContext(bicycleArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var bikePreferences = routeRequest.preferences().bike();
    assertEquals(reluctance, bikePreferences.reluctance());
    assertEquals(speed, bikePreferences.speed());
    assertEquals(boardCost.toSeconds(), bikePreferences.boardCost());
  }

  @Test
  void testBikeWalkPreferences() {
    var bicycleArgs = createArgsCopy();
    var walkSpeed = 7d;
    var mountDismountTime = Duration.ofSeconds(23);
    var mountDismountCost = Cost.costOfSeconds(35);
    var walkReluctance = 6.3;
    bicycleArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(
                entry(
                  "walk",
                  Map.ofEntries(
                    entry("speed", walkSpeed),
                    entry("mountDismountTime", mountDismountTime),
                    entry(
                      "cost",
                      Map.ofEntries(
                        entry("mountDismountCost", mountDismountCost),
                        entry("reluctance", walkReluctance)
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    );
    var env = executionContext(bicycleArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var bikeWalkingPreferences = routeRequest.preferences().bike().walking();
    assertEquals(walkSpeed, bikeWalkingPreferences.speed());
    assertEquals(mountDismountTime, bikeWalkingPreferences.mountDismountTime());
    assertEquals(mountDismountCost, bikeWalkingPreferences.mountDismountCost());
    assertEquals(walkReluctance, bikeWalkingPreferences.reluctance());
  }

  @Test
  void testBikeTrianglePreferences() {
    var bicycleArgs = createArgsCopy();
    var bikeSafety = 0.3;
    var bikeFlatness = 0.5;
    var bikeTime = 0.2;
    bicycleArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(
                entry(
                  "optimization",
                  Map.ofEntries(
                    entry(
                      "triangle",
                      Map.ofEntries(
                        entry("safety", bikeSafety),
                        entry("flatness", bikeFlatness),
                        entry("time", bikeTime)
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    );
    var env = executionContext(bicycleArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var bikePreferences = routeRequest.preferences().bike();
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, bikePreferences.optimizeType());
    var bikeTrianglePreferences = bikePreferences.optimizeTriangle();
    assertEquals(bikeSafety, bikeTrianglePreferences.safety());
    assertEquals(bikeFlatness, bikeTrianglePreferences.slope());
    assertEquals(bikeTime, bikeTrianglePreferences.time());
  }

  @Test
  void testBikeOptimizationPreferences() {
    var bicycleArgs = createArgsCopy();
    bicycleArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(entry("optimization", Map.ofEntries(entry("type", "SAFEST_STREETS"))))
            )
          )
        )
      )
    );
    var env = executionContext(bicycleArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var bikePreferences = routeRequest.preferences().bike();
    assertEquals(VehicleRoutingOptimizeType.SAFEST_STREETS, bikePreferences.optimizeType());
  }

  @Test
  void testBikeRentalPreferences() {
    var bicycleArgs = createArgsCopy();
    var allowed = Set.of("foo", "bar");
    var banned = Set.of("not");
    var allowKeeping = true;
    var keepingCost = Cost.costOfSeconds(150);
    bicycleArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(
                entry(
                  "rental",
                  Map.ofEntries(
                    entry("allowedNetworks", allowed.stream().toList()),
                    entry("bannedNetworks", banned.stream().toList()),
                    entry(
                      "destinationBicyclePolicy",
                      Map.ofEntries(
                        entry("allowKeeping", allowKeeping),
                        entry("keepingCost", keepingCost)
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    );
    var env = executionContext(bicycleArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var bikeRentalPreferences = routeRequest.preferences().bike().rental();
    assertEquals(allowed, bikeRentalPreferences.allowedNetworks());
    assertEquals(banned, bikeRentalPreferences.bannedNetworks());
    assertEquals(allowKeeping, bikeRentalPreferences.allowArrivingInRentedVehicleAtDestination());
    assertEquals(keepingCost, bikeRentalPreferences.arrivingInRentalVehicleAtDestinationCost());
  }

  @Test
  void testBikeParkingPreferences() {
    var bicycleArgs = createArgsCopy();
    var unpreferredCost = Cost.costOfSeconds(150);
    var notFilter = List.of("wheelbender");
    var selectFilter = List.of("locker", "roof");
    var unpreferred = List.of("bad");
    var preferred = List.of("a", "b");
    bicycleArgs.put(
      "preferences",
      Map.ofEntries(
        entry(
          "street",
          Map.ofEntries(
            entry(
              "bicycle",
              Map.ofEntries(
                entry(
                  "parking",
                  Map.ofEntries(
                    entry("unpreferredCost", unpreferredCost),
                    entry(
                      "filters",
                      List.of(
                        Map.ofEntries(
                          entry("not", List.of(Map.of("tags", notFilter))),
                          entry("select", List.of(Map.of("tags", selectFilter)))
                        )
                      )
                    ),
                    entry(
                      "preferred",
                      List.of(
                        Map.ofEntries(
                          entry("not", List.of(Map.of("tags", unpreferred))),
                          entry("select", List.of(Map.of("tags", preferred)))
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    );
    var env = executionContext(bicycleArgs);
    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);
    var bikeParkingPreferences = routeRequest.preferences().bike().parking();
    assertEquals(unpreferredCost, bikeParkingPreferences.unpreferredVehicleParkingTagCost());
    assertEquals(
      "VehicleParkingFilter{not: [tags=%s], select: [tags=%s]}".formatted(notFilter, selectFilter),
      bikeParkingPreferences.filter().toString()
    );
    assertEquals(
      "VehicleParkingFilter{not: [tags=%s], select: [tags=%s]}".formatted(unpreferred, preferred),
      bikeParkingPreferences.preferred().toString()
    );
  }

  private Map<String, Object> createArgsCopy() {
    Map<String, Object> newArgs = new HashMap<>();
    newArgs.putAll(args);
    return newArgs;
  }

  private DataFetchingEnvironment executionContext(Map<String, Object> arguments) {
    ExecutionInput executionInput = ExecutionInput
      .newExecutionInput()
      .query("")
      .operationName("planConnection")
      .context(context)
      .locale(LOCALE)
      .build();

    var executionContext = newExecutionContextBuilder()
      .executionInput(executionInput)
      .executionId(ExecutionId.from(this.getClass().getName()))
      .build();
    return DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .arguments(arguments)
      .localContext(Map.of("locale", LOCALE))
      .build();
  }
}
