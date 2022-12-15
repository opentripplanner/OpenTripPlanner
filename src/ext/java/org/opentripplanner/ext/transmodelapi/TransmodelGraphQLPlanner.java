package org.opentripplanner.ext.transmodelapi;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.opentripplanner.ext.transmodelapi.mapping.TripRequestMapper;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;
import org.opentripplanner.ext.transmodelapi.model.plan.ViaPlanResponse;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
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
    } catch (Exception e) {
      LOG.error("System error: " + e.getMessage(), e);
      response.plan = TripPlanMapper.mapTripPlan(request, List.of());
      response.messages.add(new RoutingError(RoutingErrorCode.SYSTEM_ERROR, null));
    }
    Locale locale = request == null ? serverContext.defaultLocale() : request.locale();
    return DataFetcherResult
      .<PlanResponse>newResult()
      .data(response)
      .localContext(Map.of("locale", locale))
      .build();
  }

  public DataFetcherResult<ViaPlanResponse> planVia(DataFetchingEnvironment environment) {
    ViaPlanResponse response;
    TransmodelRequestContext ctx = environment.getContext();
    OtpServerRequestContext serverContext = ctx.getServerContext();
    RouteViaRequest request = null;
    try {
      request = TripRequestMapper.createRouteViaRequest(environment);
      ViaRoutingResponse res = ctx.getRoutingService().route(request);
      response = ViaPlanResponse.of(res);
    } catch (Exception e) {
      LOG.error("System error: " + e.getMessage(), e);
      response = ViaPlanResponse.failed(new RoutingError(RoutingErrorCode.SYSTEM_ERROR, null));
    }
    Locale locale = request == null ? serverContext.defaultLocale() : request.locale();
    return DataFetcherResult
      .<ViaPlanResponse>newResult()
      .data(response)
      .localContext(Map.of("locale", locale))
      .build();
  }
}
