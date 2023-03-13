package org.opentripplanner.ext.carhailing;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.opentripplanner.ext.carhailing.model.ArrivalTime;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.StreetLegBuilder;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.DirectStreetRouter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.search.TraverseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CarHailingRouter {

  private static final Logger LOG = LoggerFactory.getLogger(CarHailingRouter.class);

  public static List<Itinerary> routeDirect(
    OtpServerRequestContext serverContext,
    RouteRequest routeRequest
  ) {
    if (routeRequest.journey().modes().directMode != StreetMode.CAR_HAILING) {
      return List.of();
    }
    var times = earliestArrivalTimes(routeRequest, serverContext.carHailingServices());

    return times
      .parallelStream()
      .flatMap(arrival -> {
        var req = routeRequest.clone();
        req.setDateTime(req.dateTime().plus(arrival.estimatedDuration()));

        return DirectStreetRouter
          .route(serverContext, req)
          .stream()
          .map(i -> CarHailingRouter.addCarHailInformation(i, arrival.company().name()));
      })
      .toList();
  }

  public static Itinerary addCarHailInformation(Itinerary input, String network) {
    var legs = input.getLegs().stream().map(l -> CarHailingRouter.addNetwork(l, network)).toList();
    input.setLegs(legs);
    return input;
  }

  private static List<ArrivalTime> earliestArrivalTimes(
    RouteRequest routeRequest,
    List<CarHailingService> services
  ) {
    return routeRequest
      .from()
      .toWgsCoordinate()
      .map(c ->
        services
          .parallelStream()
          .flatMap(service -> {
            try {
              return service.getArrivalTimes(c).stream();
            } catch (ExecutionException e) {
              return Stream.empty();
            }
          })
      )
      .orElse(Stream.empty())
      .toList();
  }

  private static Leg addNetwork(Leg leg, String network) {
    if (leg instanceof StreetLeg sl && sl.getMode() == TraverseMode.CAR) {
      return StreetLegBuilder.of(sl).withCarHailingNetwork(network).build();
    } else {
      return leg;
    }
  }
}
