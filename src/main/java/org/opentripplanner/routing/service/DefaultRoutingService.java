package org.opentripplanner.routing.service;

import java.time.Instant;
import java.time.ZoneId;
import org.opentripplanner.ext.ridehailing.RideHailingDepartureTimeShifter;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.algorithm.via.ViaRoutingWorker;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
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
    this.timeZone = serverContext.transitService().getTimeZone();
  }

  @Override
  public RoutingResponse route(RouteRequest request) {
    // we have to shift the start time of a car hailing request because often we cannot leave right
    // away
    if (RideHailingDepartureTimeShifter.shouldShift(request, Instant.now())) {
      var shiftingResult = RideHailingDepartureTimeShifter.shiftDepartureTime(
        request,
        serverContext.rideHailingServices(),
        Instant.now()
      );
      if (shiftingResult.isSuccess()) {
        request = shiftingResult.successValue();
      } else {
        LOG.error(
          "Could not fetch arrival time for car hailing service: {}",
          shiftingResult.failureValue()
        );
        return RoutingResponse.ofError(
          new RoutingError(RoutingErrorCode.SYSTEM_ERROR, InputField.FROM_PLACE)
        );
      }
    }
    RoutingWorker worker = new RoutingWorker(serverContext, request, timeZone);
    return worker.route();
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
