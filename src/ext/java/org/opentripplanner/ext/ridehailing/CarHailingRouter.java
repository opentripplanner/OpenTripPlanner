package org.opentripplanner.ext.ridehailing;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.opentripplanner.ext.ridehailing.model.ArrivalTime;
import org.opentripplanner.ext.ridehailing.model.RideHailingLeg;
import org.opentripplanner.ext.ridehailing.model.RideHailingProvider;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StreetLeg;
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
          .map(i -> CarHailingRouter.addCarHailInformation(i, arrival.provider()));
      })
      .toList();
  }

  public static Itinerary addCarHailInformation(Itinerary input, RideHailingProvider provider) {
    var legs = input.getLegs().stream().map(l -> CarHailingRouter.addNetwork(l, provider)).toList();
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
              return service.arrivalTimes(c).stream();
            } catch (ExecutionException e) {
              LOG.error("Error while fetching car hailing arrival times", e);
              return Stream.empty();
            }
          })
      )
      .orElse(Stream.empty())
      .toList();
  }

  private static Leg addNetwork(Leg leg, RideHailingProvider provider) {
    if (leg instanceof StreetLeg sl && sl.getMode() == TraverseMode.CAR) {
      return new RideHailingLeg(sl, provider, null);
    } else {
      return leg;
    }
  }
}
