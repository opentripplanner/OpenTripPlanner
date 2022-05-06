package org.opentripplanner.transit.model.trip;

public class TripService {

  private final RoutingTripPattern[] routingTripPatterns;

  public TripService(RoutingTripPattern[] routingTripPatterns) {
    this.routingTripPatterns = routingTripPatterns;
  }

  public int numberOfRoutingPatterns() {
    return routingTripPatterns.length;
  }

  public RoutingTripPattern routingPatterns(int index) {
    return routingTripPatterns[index];
  }
}
