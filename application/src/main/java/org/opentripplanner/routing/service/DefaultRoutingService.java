package org.opentripplanner.routing.service;

import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.time.ZoneIdFallback;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.RequestPreProcessor;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.algorithm.RoutingWorkerRequest;
import org.opentripplanner.routing.algorithm.via.ViaRoutingWorker;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.utils.tostring.MultiLineToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO VIA: 2022-08-29 javadocs

/**
 * Entry point for requests towards the routing API.
 */
public class DefaultRoutingService implements RoutingService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultRoutingService.class);

  private final OtpServerRequestContext serverContext;

  private final RequestPreProcessor requestPreProcessor;

  public DefaultRoutingService(OtpServerRequestContext serverContext) {
    this.serverContext = serverContext;

    var timeZone = ZoneIdFallback.zoneId(serverContext.transitService().getTimeZone());

    this.requestPreProcessor = new RequestPreProcessor(
      serverContext.transitService(),
      serverContext.raptorTuningParameters(),
      timeZone
    );
  }

  @Override
  public RoutingResponse route(RouteRequest request) {
    LOG.debug("Request: {}", request);
    OTPRequestTimeoutException.checkForTimeout();
    request.validateOriginAndDestination();
    var worker = new RoutingWorker(serverContext, mapRequest(request));
    var response = worker.route();
    logResponse(response);
    return response;
  }

  @Override
  public ViaRoutingResponse route(RouteViaRequest request) {
    LOG.debug("Request: {}", request);
    OTPRequestTimeoutException.checkForTimeout();
    var viaRoutingWorker = new ViaRoutingWorker(request, req ->
      new RoutingWorker(serverContext, mapRequest(req)).route()
    );
    // TODO: Add output logging here, see route(..) method
    return viaRoutingWorker.route();
  }

  private RoutingWorkerRequest mapRequest(RouteRequest request) {
    return requestPreProcessor.computeRequest(request);
  }

  private void logResponse(RoutingResponse response) {
    if (response.getTripPlan().itineraries.isEmpty() && response.getRoutingErrors().isEmpty()) {
      // We should provide an error if there is no results, this is important for the client so
      // it knows if it can page or abort.
      LOG.warn("The routing result is empty, but there is no errors...");
    }

    if (LOG.isDebugEnabled()) {
      var text = MultiLineToStringBuilder.of("Response")
        .add("NextPage", response.getNextPageCursor())
        .add("PreviousPage", response.getPreviousPageCursor())
        .addColNl(
          "Itineraries",
          response.getTripPlan().itineraries.stream().map(Itinerary::toStr).toList()
        )
        .addColNl("Errors", response.getRoutingErrors())
        .toString();
      LOG.debug(text);
    }
  }
}
