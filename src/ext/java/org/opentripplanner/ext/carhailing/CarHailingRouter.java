package org.opentripplanner.ext.carhailing;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

  private final RouteRequest routeRequest;

  public CarHailingRouter(RouteRequest request) {
    this.routeRequest = request;
  }

  private Instant earliestArrivalTime() {
    return routeRequest.dateTime().plus(Duration.ofMinutes(15));
  }

  public List<Itinerary> routeDirect(OtpServerRequestContext serverContext) {
    if (routeRequest.journey().modes().directMode != StreetMode.CAR_HAIL) {
      return List.of();
    }
    var earliestArrivalTime = earliestArrivalTime();

    var req = routeRequest.clone();
    req.setDateTime(earliestArrivalTime);

    return DirectStreetRouter
      .route(serverContext, req)
      .stream()
      .map(CarHailingRouter::addCarHailInformation)
      .toList();
  }

  public static Itinerary addCarHailInformation(Itinerary input) {
    return null;
  }

  private static Leg foo(Leg leg) {
    if (leg instanceof StreetLeg sl) {
      return StreetLegBuilder
        .of(sl)
        .withCarHailingNetwork("uber")
        .withMode(TraverseMode.CAR)
        .build();
    } else {
      return leg;
    }
  }
}
