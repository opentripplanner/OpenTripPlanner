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
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;
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
    Locale locale;
    PlanResponse response;
    RouteRequestBuilder requestBuilder = tripRequestMapper.createRequestBuilder(environment);
    try {
      var request = requestBuilder.buildRequest();
      RoutingResponse res = ctx.getRoutingService().route(request);
      response = PlanResponse.of()
        .withPlan(res.getTripPlan())
        .withMetadata(res.getMetadata())
        .withMessages(res.getRoutingErrors())
        .withDebugOutput(res.getDebugTimingAggregator().finishedRendering())
        .withPreviousPageCursor(res.getPreviousPageCursor())
        .withNextPageCursor(res.getNextPageCursor())
        .build();
      locale = request.preferences().locale();
    } catch (RoutingValidationException e) {
      response = PlanResponse.of()
        .withPlan(TripPlanMapper.mapEmptyTripPlan(requestBuilder))
        .withMessages(e.getRoutingErrors())
        .build();
      locale = defaultLocale(ctx);
    }
    return DataFetcherResult.<PlanResponse>newResult()
      .data(response)
      .localContext(Map.of("locale", locale))
      .build();
  }

  public DataFetcherResult<ViaRoutingResponse> planVia(DataFetchingEnvironment environment) {
    ViaRoutingResponse response;
    TransmodelRequestContext ctx = environment.getContext();
    Locale locale;
    try {
      var request = viaRequestMapper.createRouteViaRequest(environment);
      response = ctx.getRoutingService().route(request);
      locale = request.locale();
    } catch (RoutingValidationException e) {
      response = new ViaRoutingResponse(Map.of(), List.of(), e.getRoutingErrors());
      locale = defaultLocale(ctx);
    }
    return DataFetcherResult.<ViaRoutingResponse>newResult()
      .data(response)
      .localContext(Map.of("locale", locale))
      .build();
  }

  private static Locale defaultLocale(TransmodelRequestContext ctx) {
    return ctx.getServerContext().defaultRouteRequest().preferences().locale();
  }
}
