package org.opentripplanner.routing.service;

import java.time.ZoneId;
import org.opentripplanner.ext.ridehailing.RideHailingDepartureTimeShifter;
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

  public final RequestModifier requestModifier;

  public DefaultRoutingService(OtpServerRequestContext serverContext) {
    this.serverContext = serverContext;
    this.timeZone = serverContext.transitService().getTimeZone();
    if (serverContext.rideHailingServices().isEmpty()) {
      this.requestModifier = RequestModifier.NOOP;
    } else {
      this.requestModifier = RideHailingDepartureTimeShifter::modifyRequest;
    }
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
