package org.opentripplanner.ext.legacygraphqlapi.mapping;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.routing.core.BicycleOptimizeType.SAFE;
import static org.opentripplanner.routing.core.BicycleOptimizeType.TRIANGLE;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.TestRoutingService;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.service.vehiclepositions.internal.DefaultVehiclePositionService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

class RouteRequestMapperTest implements PlanTestConstants {

  static final LegacyGraphQLRequestContext context;

  static {
    Graph graph = new Graph();
    var transitModel = new TransitModel();
    transitModel.initTimeZone(ZoneIds.BERLIN);
    final DefaultTransitService transitService = new DefaultTransitService(transitModel);
    context =
      new LegacyGraphQLRequestContext(
        new TestRoutingService(List.of()),
        transitService,
        new DefaultFareService(),
        graph.getVehicleParkingService(),
        new DefaultVehicleRentalService(),
        new DefaultVehiclePositionService(),
        GraphFinder.getInstance(graph, transitService::findRegularStop),
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

    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);

    assertNotNull(routeRequest);

    final VehicleParkingRequest parking = routeRequest.journey().parking();
    assertEquals(
      "VehicleParkingFilterRequest{not: [tags=[wheelbender]], select: [tags=[locker, roof]]}",
      parking.filter().toString()
    );
    assertEquals(
      "VehicleParkingFilterRequest{select: [tags=[a, b]]}",
      parking.preferred().toString()
    );
    assertEquals(555, parking.unpreferredCost());
  }

  static Stream<Arguments> banningCases = Stream.of(
    of(Map.of(), "[TransitFilterRequest{}]"),
    of(
      Map.of("routes", "trimet:555"),
      "[TransitFilterRequest{not: [SelectRequest{transportModes: [], routes: [trimet:555]}]}]"
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

  @ParameterizedTest
  @VariableSource("banningCases")
  void banning(Map<String, Object> banned, String expectedFilters) {
    Map<String, Object> arguments = Map.of("banned", banned);

    var routeRequest = RouteRequestMapper.toRouteRequest(executionContext(arguments), context);
    assertNotNull(routeRequest);

    assertEquals(expectedFilters, routeRequest.journey().transit().filters().toString());
  }

  static Stream<Arguments> transportModesCases = Stream.of(
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

  @ParameterizedTest
  @VariableSource("transportModesCases")
  void modes(List<Map<String, Object>> modes, String expectedFilters) {
    Map<String, Object> arguments = Map.of("transportModes", modes);

    var routeRequest = RouteRequestMapper.toRouteRequest(executionContext(arguments), context);
    assertNotNull(routeRequest);

    assertEquals(expectedFilters, routeRequest.journey().transit().filters().toString());
  }

  private static Map<String, Object> mode(String mode) {
    return Map.of("mode", mode);
  }

  @Test
  void defaultBikeOptimize() {
    Map<String, Object> arguments = Map.of();
    var routeRequest = RouteRequestMapper.toRouteRequest(executionContext(arguments), context);
    assertEquals(SAFE, routeRequest.preferences().bike().optimizeType());
  }

  @Test
  void bikeTriangle() {
    Map<String, Object> arguments = Map.of(
      "optimize",
      "TRIANGLE",
      "triangle",
      Map.of("safetyFactor", 0.2, "slopeFactor", 0.1, "timeFactor", 0.7)
    );

    var routeRequest = RouteRequestMapper.toRouteRequest(executionContext(arguments), context);

    assertEquals(TRIANGLE, routeRequest.preferences().bike().optimizeType());
    assertEquals(
      new TimeSlopeSafetyTriangle(0.7, 0.1, 0.2),
      routeRequest.preferences().bike().optimizeTriangle()
    );
  }

  static Stream<Arguments> noTriangleCases = BicycleOptimizeType
    .nonTriangleValues()
    .stream()
    .map(Arguments::of);

  @ParameterizedTest
  @VariableSource("noTriangleCases")
  void noTriangle(BicycleOptimizeType bot) {
    Map<String, Object> arguments = Map.of(
      "optimize",
      bot.name(),
      "triangle",
      Map.of("safetyFactor", 0.2, "slopeFactor", 0.1, "timeFactor", 0.7)
    );

    var routeRequest = RouteRequestMapper.toRouteRequest(executionContext(arguments), context);

    assertEquals(bot, routeRequest.preferences().bike().optimizeType());
    assertEquals(
      TimeSlopeSafetyTriangle.DEFAULT,
      routeRequest.preferences().bike().optimizeTriangle()
    );
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
}
