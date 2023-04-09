package org.opentripplanner.routing.service;

import java.time.ZoneId;
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

  /**
   * Modifies the request before sending it to the routing engine.
   */
  public final RequestModifier requestModifier;

  public DefaultRoutingService(OtpServerRequestContext serverContext, RequestModifier modifier) {
    this.serverContext = serverContext;
    this.timeZone = serverContext.transitService().getTimeZone();
    this.requestModifier = modifier;
  }

  @Override
  public RoutingResponse route(RouteRequest request) {
    var modificationResult = requestModifier.modify(serverContext, request);

    if (modificationResult.isSuccess()) {
      RoutingWorker worker = new RoutingWorker(
        serverContext,
        modificationResult.successValue(),
        timeZone
      );
      return worker.route();
    } else {
      return RoutingResponse.ofError(modificationResult.failureValue());
    }
  }

  @Override
  public ViaRoutingResponse route(RouteViaRequest request) {
    var viaRoutingWorker = new ViaRoutingWorker(
      request,
      req ->
        new RoutingWorker(serverContext, req, serverContext.transitService().getTimeZone()).route()
    );
    return viaRoutingWorker.route();
  }
}
