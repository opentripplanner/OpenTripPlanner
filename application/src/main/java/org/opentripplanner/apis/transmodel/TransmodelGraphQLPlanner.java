package org.opentripplanner.apis.transmodel;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.opentripplanner.apis.transmodel.mapping.TripRequestMapper;
import org.opentripplanner.apis.transmodel.mapping.ViaRequestMapper;
import org.opentripplanner.apis.transmodel.model.PlanResponse;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransmodelGraphQLPlanner {

  private static final Logger LOG = LoggerFactory.getLogger(TransmodelGraphQLPlanner.class);

  public DataFetcherResult<PlanResponse> plan(DataFetchingEnvironment environment) {
    PlanResponse response = new PlanResponse();
    TransmodelRequestContext ctx = environment.getContext();
    OtpServerRequestContext serverContext = ctx.getServerContext();
    RouteRequest request = null;
    try {
      request = TripRequestMapper.createRequest(environment);
      RoutingResponse res = ctx.getRoutingService().route(request);

      response.plan = res.getTripPlan();
      response.metadata = res.getMetadata();
      response.messages = res.getRoutingErrors();
      response.debugOutput = res.getDebugTimingAggregator().finishedRendering();
      response.previousPageCursor = res.getPreviousPageCursor();
      response.nextPageCursor = res.getNextPageCursor();
    } catch (RoutingValidationException e) {
      response.plan = TripPlanMapper.mapTripPlan(request, List.of());
      response.messages.addAll(e.getRoutingErrors());
    }

    Locale locale = request == null ? serverContext.defaultLocale() : request.locale();
    return DataFetcherResult
      .<PlanResponse>newResult()
      .data(response)
      .localContext(Map.of("locale", locale))
      .build();
  }

  public DataFetcherResult<ViaRoutingResponse> planVia(DataFetchingEnvironment environment) {
    ViaRoutingResponse response;
    TransmodelRequestContext ctx = environment.getContext();
    RouteViaRequest request = null;
    try {
      request = ViaRequestMapper.createRouteViaRequest(environment);
      response = ctx.getRoutingService().route(request);
    } catch (RoutingValidationException e) {
      response = new ViaRoutingResponse(Map.of(), List.of(), e.getRoutingErrors());
    }

    Locale defaultLocale = ctx.getServerContext().defaultLocale();
    Locale locale = request == null ? defaultLocale : request.locale();
    return DataFetcherResult
      .<ViaRoutingResponse>newResult()
      .data(response)
      .localContext(Map.of("locale", locale))
      .build();
  }
}
