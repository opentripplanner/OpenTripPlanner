package org.opentripplanner.apis.transmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.apis.support.graphql.DataFetchingSupport.executionContext;

import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.routing.error.InvalidRoutingInputException;
import org.opentripplanner.routing.error.RoutingValidationException;

/**
 * Regression test: {@link TransmodelGraphQLPlanner#plan} must keep {@code buildRequest()} inside
 * the try block so routing/validation exceptions from an invalid request are still turned into a
 * graceful empty-plan response or a mapped {@link InvalidInputException}, not an uncaught crash.
 */
class TransmodelGraphQLPlannerTest {

  private static final TransmodelGraphQLPlanner PLANNER = new TransmodelGraphQLPlanner(
    new DefaultFeedIdMapper()
  );

  @Test
  void routingValidationExceptionProducesEmptyPlanNotACrash() {
    var routingService = routingServiceThrowing(
      new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.NO_TRANSIT_CONNECTION, null))
      )
    );

    var result = PLANNER.plan(environment(routingService));

    assertEquals(List.of(), result.getData().itineraries());
    assertEquals(1, result.getData().messages().size());
  }

  @Test
  void invalidRoutingInputExceptionIsRemappedNotThrownRaw() {
    var routingService = routingServiceThrowing(new InvalidRoutingInputException("bad input"));

    assertThrows(InvalidInputException.class, () -> PLANNER.plan(environment(routingService)));
  }

  private static RoutingService routingServiceThrowing(RuntimeException e) {
    return new RoutingService() {
      @Override
      public RoutingResponse route(RouteRequest request) {
        throw e;
      }

      @Override
      public ViaRoutingResponse route(RouteViaRequest request) {
        throw new UnsupportedOperationException();
      }
    };
  }

  private static graphql.schema.DataFetchingEnvironment environment(RoutingService routingService) {
    var defaultRouteRequest = RouteRequest.of()
      .withFrom(org.opentripplanner.model.GenericLocation.fromCoordinate(0, 0))
      .withTo(org.opentripplanner.model.GenericLocation.fromCoordinate(1, 1))
      .buildRequest();

    var context = new TestTransmodelGraphQLRequestContext(
      routingService,
      null,
      null,
      null,
      defaultRouteRequest,
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );

    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext())
      .context(context)
      .build();
  }
}
