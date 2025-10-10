package org.opentripplanner.apis.transmodel;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.mapping.TripRequestMapper;
import org.opentripplanner.apis.transmodel.mapping.ViaRequestMapper;
import org.opentripplanner.apis.transmodel.model.PlanResponse;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransmodelGraphQLPlanner {

  private static final Logger LOG = LoggerFactory.getLogger(TransmodelGraphQLPlanner.class);

  private final TripRequestMapper tripRequestMapper;
  private final ViaRequestMapper viaRequestMapper;

  public TransmodelGraphQLPlanner(FeedScopedIdMapper idMapper) {
    this.tripRequestMapper = new TripRequestMapper(idMapper);
    this.viaRequestMapper = new ViaRequestMapper(idMapper);
  }

  public DataFetcherResult<PlanResponse> plan(DataFetchingEnvironment environment) {
    TransmodelRequestContext ctx = environment.getContext();
    RouteRequest request = null;
    PlanResponse response;

    try {
      request = tripRequestMapper.createRequest(environment);
      RoutingResponse res = ctx.getRoutingService().route(request);

      response = PlanResponse.builder()
        .withPlan(res.getTripPlan())
        .withMetadata(res.getMetadata())
        .withMessages(res.getRoutingErrors())
        .withDebugOutput(res.getDebugTimingAggregator().finishedRendering())
        .withPreviousPageCursor(res.getPreviousPageCursor())
        .withNextPageCursor(res.getNextPageCursor())
        .build();
    } catch (RoutingValidationException e) {
      response = PlanResponse.ofErrors(e.getRoutingErrors());
    }
    // The request can be null if the request mapper encounters a RoutingValidationException.
    Locale locale = request == null ? defaultLocale(ctx) : request.preferences().locale();
    return DataFetcherResult.<PlanResponse>newResult()
      .data(response)
      .localContext(Map.of("locale", locale))
      .build();
  }

  public DataFetcherResult<ViaRoutingResponse> planVia(DataFetchingEnvironment environment) {
    ViaRoutingResponse response;
    TransmodelRequestContext ctx = environment.getContext();
    RouteViaRequest request = null;
    try {
      request = viaRequestMapper.createRouteViaRequest(environment);
      response = ctx.getRoutingService().route(request);
    } catch (RoutingValidationException e) {
      response = new ViaRoutingResponse(Map.of(), List.of(), e.getRoutingErrors());
    }

    // The request can be null if the request mapper encounters a RoutingValidationException.
    Locale locale = request == null ? defaultLocale(ctx) : request.locale();
    return DataFetcherResult.<ViaRoutingResponse>newResult()
      .data(response)
      .localContext(Map.of("locale", locale))
      .build();
  }

  private static Locale defaultLocale(TransmodelRequestContext ctx) {
    return ctx.getServerContext().defaultRouteRequest().preferences().locale();
  }
}
