package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.routing.core.VehicleRoutingOptimizeType.SAFE_STREETS;
import static org.opentripplanner.routing.core.VehicleRoutingOptimizeType.TRIANGLE;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.TestRoutingService;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.routing.api.request.preference.TransferPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

class LegacyRouteRequestMapperTest implements PlanTestConstants {

  static final GraphQLRequestContext context;

  static {
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
  void parkingFilters() {
    Map<String, Object> arguments = Map.of(
      "parking",
      Map.of(
        "unpreferredCost",
        555,
        "filters",
        List.of(
          Map.of(
            "not",
            List.of(Map.of("tags", List.of("wheelbender"))),
            "select",
            List.of(Map.of("tags", List.of("roof", "locker")))
          )
        ),
        "preferred",
        List.of(Map.of("select", List.of(Map.of("tags", List.of("a", "b")))))
      )
    );

    var env = executionContext(arguments);

    var routeRequest = LegacyRouteRequestMapper.toRouteRequest(env, context);

    assertNotNull(routeRequest);

    testParkingFilters(routeRequest.preferences().parking(TraverseMode.CAR));
    testParkingFilters(routeRequest.preferences().parking(TraverseMode.BICYCLE));
  }

  static Stream<Arguments> banningCases() {
    return Stream.of(
      of(Map.of(), "[TransitFilterRequest{}]"),
      of(
        Map.of("routes", "trimet:555"),
        "[TransitFilterRequest{not: [SelectRequest{transportModes: [], routes: [trimet:555]}]}]"
      ),
      of(
        Map.of("agencies", ""),
        "[TransitFilterRequest{not: [SelectRequest{transportModes: []}]}]"
      ),
      of(
        Map.of("agencies", "trimet:666"),
        "[TransitFilterRequest{not: [SelectRequest{transportModes: [], agencies: [trimet:666]}]}]"
      ),
      of(
        Map.of("agencies", "trimet:666", "routes", "trimet:444"),
        "[TransitFilterRequest{not: [SelectRequest{transportModes: [], routes: [trimet:444]}, SelectRequest{transportModes: [], agencies: [trimet:666]}]}]"
      )
    );
  }

  @ParameterizedTest
  @MethodSource("banningCases")
  void banning(Map<String, Object> banned, String expectedFilters) {
    Map<String, Object> arguments = Map.of("banned", banned);

    var routeRequest = LegacyRouteRequestMapper.toRouteRequest(
      executionContext(arguments),
      context
    );
    assertNotNull(routeRequest);

    assertEquals(expectedFilters, routeRequest.journey().transit().filters().toString());
  }

  static Stream<Arguments> transportModesCases() {
    return Stream.of(
      of(List.of(), "[ExcludeAllTransitFilter{}]"),
      of(List.of(mode("BICYCLE")), "[ExcludeAllTransitFilter{}]"),
      of(
        List.of(mode("BUS")),
        "[TransitFilterRequest{select: [SelectRequest{transportModes: [BUS, COACH]}]}]"
      ),
      of(
        List.of(mode("BUS"), mode("MONORAIL")),
        "[TransitFilterRequest{select: [SelectRequest{transportModes: [BUS, COACH, MONORAIL]}]}]"
      )
    );
  }

  @ParameterizedTest
  @MethodSource("transportModesCases")
  void modes(List<Map<String, Object>> modes, String expectedFilters) {
    Map<String, Object> arguments = Map.of("transportModes", modes);

    var routeRequest = LegacyRouteRequestMapper.toRouteRequest(
      executionContext(arguments),
      context
    );
    assertNotNull(routeRequest);

    assertEquals(expectedFilters, routeRequest.journey().transit().filters().toString());
  }

  private static Map<String, Object> mode(String mode) {
    return Map.of("mode", mode);
  }

  @Test
  void defaultBikeOptimize() {
    Map<String, Object> arguments = Map.of();
    var routeRequest = LegacyRouteRequestMapper.toRouteRequest(
      executionContext(arguments),
      context
    );
    assertEquals(SAFE_STREETS, routeRequest.preferences().bike().optimizeType());
  }

  @Test
  void bikeTriangle() {
    Map<String, Object> arguments = Map.of(
      "optimize",
      "TRIANGLE",
      "triangle",
      Map.of("safetyFactor", 0.2, "slopeFactor", 0.1, "timeFactor", 0.7)
    );

    var routeRequest = LegacyRouteRequestMapper.toRouteRequest(
      executionContext(arguments),
      context
    );

    assertEquals(TRIANGLE, routeRequest.preferences().bike().optimizeType());
    assertEquals(
      new TimeSlopeSafetyTriangle(0.7, 0.1, 0.2),
      routeRequest.preferences().bike().optimizeTriangle()
    );
  }

  static Stream<Arguments> noTriangleCases() {
    return Arrays
      .stream(GraphQLTypes.GraphQLOptimizeType.values())
      .filter(value -> value != GraphQLTypes.GraphQLOptimizeType.TRIANGLE)
      .map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("noTriangleCases")
  void noTriangle(GraphQLTypes.GraphQLOptimizeType bot) {
    Map<String, Object> arguments = Map.of(
      "optimize",
      bot.name(),
      "triangle",
      Map.of("safetyFactor", 0.2, "slopeFactor", 0.1, "timeFactor", 0.7)
    );

    var routeRequest = LegacyRouteRequestMapper.toRouteRequest(
      executionContext(arguments),
      context
    );

    assertEquals(OptimizationTypeMapper.map(bot), routeRequest.preferences().bike().optimizeType());
    assertEquals(
      TimeSlopeSafetyTriangle.DEFAULT,
      routeRequest.preferences().bike().optimizeTriangle()
    );
  }

  @Test
  void walkReluctance() {
    var reluctance = 119d;
    Map<String, Object> arguments = Map.of("walkReluctance", reluctance);

    var routeRequest = LegacyRouteRequestMapper.toRouteRequest(
      executionContext(arguments),
      context
    );
    assertEquals(reluctance, routeRequest.preferences().walk().reluctance());

    var noParamsRequest = LegacyRouteRequestMapper.toRouteRequest(
      executionContext(Map.of()),
      context
    );
    assertNotEquals(reluctance, noParamsRequest.preferences().walk().reluctance());
  }

  @Test
  void transferSlack() {
    var seconds = 119L;
    Map<String, Object> arguments = Map.of("minTransferTime", seconds);

    var routeRequest = LegacyRouteRequestMapper.toRouteRequest(
      executionContext(arguments),
      context
    );
    assertEquals(Duration.ofSeconds(seconds), routeRequest.preferences().transfer().slack());

    var noParamsReq = LegacyRouteRequestMapper.toRouteRequest(executionContext(Map.of()), context);
    assertEquals(TransferPreferences.DEFAULT.slack(), noParamsReq.preferences().transfer().slack());
  }

  private DataFetchingEnvironment executionContext(Map<String, Object> arguments) {
    ExecutionInput executionInput = ExecutionInput
      .newExecutionInput()
      .query("")
      .operationName("plan")
      .context(context)
      .locale(Locale.ENGLISH)
      .build();

    var executionContext = newExecutionContextBuilder()
      .executionInput(executionInput)
      .executionId(ExecutionId.from(this.getClass().getName()))
      .build();
    return DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .arguments(arguments)
      .build();
  }

  private void testParkingFilters(VehicleParkingPreferences parkingPreferences) {
    assertEquals(
      "VehicleParkingFilter{not: [tags=[wheelbender]], select: [tags=[locker, roof]]}",
      parkingPreferences.filter().toString()
    );
    assertEquals(
      "VehicleParkingFilter{select: [tags=[a, b]]}",
      parkingPreferences.preferred().toString()
    );
    assertEquals(555, parkingPreferences.unpreferredVehicleParkingTagCost().toSeconds());
  }
}
