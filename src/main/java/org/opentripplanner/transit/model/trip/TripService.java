package org.opentripplanner.transit.model.trip;

import org.opentripplanner.transit.model.network.RoutingTripPatternV2;

public class TripService {

  private final RoutingTripPatternV2[] routingTripPatterns;

  public TripService(RoutingTripPatternV2[] routingTripPatterns) {
    this.routingTripPatterns = routingTripPatterns;
  }

  public int numberOfRoutingPatterns() {
    return routingTripPatterns.length;
  }

  public RoutingTripPatternV2 routingPatterns(int index) {
    return routingTripPatterns[index];
  }
}
