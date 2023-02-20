package org.opentripplanner.ext.legacygraphqlapi;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.ext.legacygraphqlapi.mapping.RouteRequestMapper;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalService;
import org.opentripplanner.service.vehiclepositions.internal.DefaultVehiclePositionService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

class GraphQLPlanTest implements PlanTestConstants {

  static final LegacyGraphQLRequestContext context;
  static final Graph graph = new Graph();
  static final TestRoutingService routingService = new TestRoutingService(List.of());

  static {
    var transitModel = new TransitModel();
    transitModel.initTimeZone(ZoneIds.BERLIN);
    final DefaultTransitService transitService = new DefaultTransitService(transitModel);
    context =
      new LegacyGraphQLRequestContext(
        routingService,
        transitService,
        new DefaultFareService(),
        graph.getVehicleParkingService(),
        new VehicleRentalService(),
        new DefaultVehiclePositionService(),
        GraphFinder.getInstance(graph, transitService::findRegularStop),
        new RouteRequest()
      );
  }

  @Test
  void preferredVehicleParkingTags() {
    var query =
      """
      query {
        plan(
          parking: {
            filters: { not : { tags: ["forbiddentag"] }},
          }
        ) {
          itineraries {
            duration
          }
        }
      }
      
      """;

    ExecutionInput executionInput = ExecutionInput
      .newExecutionInput()
      .query(query)
      .operationName("plan")
      .context(context)
      .locale(Locale.ENGLISH)
      .build();

    var executionContext = newExecutionContextBuilder()
      .executionInput(executionInput)
      .executionId(ExecutionId.from(this.getClass().getName()))
      .build();

    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext).build();

    var routeRequest = RouteRequestMapper.toRouteRequest(env, context);

    assertNotNull(routeRequest);

    assertEquals(Set.of("forbiddentag"), routeRequest.journey().parking().bannedTags());
  }
}
