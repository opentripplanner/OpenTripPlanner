package org.opentripplanner.routing.service;

import java.time.ZoneId;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.time.ZoneIdFallback;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.RoutingWorker;
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

  private final ZoneId timeZone;

  public DefaultRoutingService(OtpServerRequestContext serverContext) {
    this.serverContext = serverContext;
    this.timeZone = ZoneIdFallback.zoneId(serverContext.transitService().getTimeZone());
  }

  @Override
  public RoutingResponse route(RouteRequest request) {
    LOG.debug("Request: {}", request);
    OTPRequestTimeoutException.checkForTimeout();
    request.validateOriginAndDestination();
    var worker = new RoutingWorker(serverContext, request, timeZone);
    var response = worker.route();
    logResponse(response);
    return response;
  }

  @Override
  public ViaRoutingResponse route(RouteViaRequest request) {
    LOG.debug("Request: {}", request);
    OTPRequestTimeoutException.checkForTimeout();
    var viaRoutingWorker = new ViaRoutingWorker(request, req ->
      new RoutingWorker(serverContext, req, serverContext.transitService().getTimeZone()).route()
    );
    // TODO: Add output logging here, see route(..) method
    return viaRoutingWorker.route();
  }

  private void logResponse(RoutingResponse response) {
    if (response.getTripPlan().itineraries.isEmpty() && response.getRoutingErrors().isEmpty()) {
      // We should provide an error if there is no results, this is important for the client so
      // it knows if it can page or abort.
      LOG.warn("The routing result is empty, but there is no errors...");
    }

    if (LOG.isDebugEnabled()) {
      var m = response.getMetadata();
      var text = MultiLineToStringBuilder.of("Response")
        .addDuration("SearchWindowUsed", m == null ? null : m.searchWindowUsed)
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
