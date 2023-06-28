package org.opentripplanner.routing.service;

import java.time.ZoneId;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.algorithm.via.ViaRoutingWorker;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

// TODO VIA: 2022-08-29 javadocs

/**
 * Entry point for requests towards the routing API.
 */
public class DefaultRoutingService implements RoutingService {

  private final OtpServerRequestContext serverContext;

  private final ZoneId timeZone;

  public DefaultRoutingService(OtpServerRequestContext serverContext) {
    this.serverContext = serverContext;
    this.timeZone = serverContext.transitService().getTimeZone();
  }

  @Override
  public RoutingResponse route(RouteRequest request) {
    OTPRequestTimeoutException.checkForTimeout();
    request.validateOriginAndDestination();
    RoutingWorker worker = new RoutingWorker(serverContext, request, timeZone);
    return worker.route();
  }

  @Override
  public ViaRoutingResponse route(RouteViaRequest request) {
    OTPRequestTimeoutException.checkForTimeout();
    var viaRoutingWorker = new ViaRoutingWorker(
      request,
      req ->
        new RoutingWorker(serverContext, req, serverContext.transitService().getTimeZone()).route()
    );
    return viaRoutingWorker.route();
  }
}
