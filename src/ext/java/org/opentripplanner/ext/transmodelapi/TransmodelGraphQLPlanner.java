package org.opentripplanner.ext.transmodelapi;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.opentripplanner.ext.transmodelapi.mapping.TripRequestMapper;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;
import org.opentripplanner.ext.transmodelapi.model.plan.ViaPlanResponse;
import org.opentripplanner.model.plan.Itinerary;
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

      final List<List<Itinerary>> viaJourneys = res
        .routingResponses()
        .stream()
        .map(RoutingResponse::getTripPlan)
        .map(plan -> plan.itineraries)
        .toList();

      List<List<List<Integer>>> connectionLists = createConnections(res, viaJourneys);
      response = new ViaPlanResponse(viaJourneys, connectionLists, res.routingErrors());
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

  private static List<List<List<Integer>>> createConnections(
    ViaRoutingResponse res,
    List<List<Itinerary>> viaJourneys
  ) {
    var connectionLists = new ArrayList<List<List<Integer>>>();

    for (int i = 0; i < viaJourneys.size() - 1; i++) {
      var connectionList = new ArrayList<List<Integer>>();
      connectionLists.add(connectionList);
      List<Itinerary> itineraries = viaJourneys.get(i);
      List<Itinerary> nextItineraries = viaJourneys.get(i + 1);
      for (var itinerary : itineraries) {
        var currentConnections = new ArrayList<Integer>();
        connectionList.add(currentConnections);
        var connections = res.plan().get(itinerary);
        if (connections != null) {
          for (var connection : connections) {
            var index = nextItineraries.indexOf(connection);
            if (index != -1) {
              currentConnections.add(index);
            }
          }
        }
      }
    }
    return connectionLists;
  }
}
