package org.opentripplanner.ext.carhailing;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.StreetLegBuilder;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.DirectStreetRouter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.search.TraverseMode;

public class CarHailingRouter {

  public static List<Itinerary> routeDirect(
    OtpServerRequestContext serverContext,
    RouteRequest routeRequest
  ) {
    if (routeRequest.journey().modes().directMode != StreetMode.CAR_HAIL) {
      return List.of();
    }
    var times = earliestArrivalTimes(routeRequest);

    return times
      .entrySet()
      .parallelStream()
      .flatMap(entry -> {
        var req = routeRequest.clone();
        req.setDateTime(entry.getValue());

        return DirectStreetRouter
          .route(serverContext, req)
          .stream()
          .map(i -> CarHailingRouter.addCarHailInformation(i, entry.getKey()));
      })
      .toList();
  }

  public static Itinerary addCarHailInformation(Itinerary input, String network) {
    var legs = input.getLegs().stream().map(l -> CarHailingRouter.addNetwork(l, network)).toList();
    input.setLegs(legs);
    return input;
  }

  private static Map<String, Instant> earliestArrivalTimes(RouteRequest routeRequest) {
    return Map.of("uber", routeRequest.dateTime().plus(Duration.ofMinutes(15)));
  }

  private static Leg addNetwork(Leg leg, String network) {
    if (leg instanceof StreetLeg sl && sl.getMode() == TraverseMode.CAR) {
      return StreetLegBuilder.of(sl).withCarHailingNetwork(network).build();
    } else {
      return leg;
    }
  }
}
