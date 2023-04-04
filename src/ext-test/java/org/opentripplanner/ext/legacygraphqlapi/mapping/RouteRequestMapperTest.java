package org.opentripplanner.ext.legacygraphqlapi.mapping;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.TestRoutingService;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.service.vehiclepositions.internal.DefaultVehiclePositionService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
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

    var env = DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .arguments(arguments)
      .build();

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
}
