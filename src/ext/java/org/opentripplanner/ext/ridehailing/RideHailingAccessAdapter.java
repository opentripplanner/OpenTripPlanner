package org.opentripplanner.ext.ridehailing;

import java.time.Duration;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;

/**
 * This class is used to adapt the ride hailing accesses (not egresses) into a time-dependent
 * multi-leg {@link DefaultAccessEgress}.
 */
public final class RideHailingAccessAdapter extends DefaultAccessEgress {

  private final Duration arrival;

  public RideHailingAccessAdapter(DefaultAccessEgress access, Duration arrival) {
    super(access.stop(), access.getLastState());
    this.arrival = arrival;
  }

  public RideHailingAccessAdapter(RideHailingAccessAdapter other, TimeAndCost penalty) {
    super(other, penalty);
    this.arrival = other.arrival;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    return super.earliestDepartureTime(requestedDepartureTime) + (int) arrival.toSeconds();
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    return super.latestArrivalTime(requestedArrivalTime) + (int) arrival.toSeconds();
  }

  @Override
  public int numberOfRides() {
    // We only support one leg at the moment
    return 1;
  }

  @Override
  public boolean hasOpeningHours() {
    return true;
  }

  @Override
  public String openingHoursToString() {
    return "Arrival in " + arrival.toString();
  }

  @Override
  public DefaultAccessEgress withPenalty(TimeAndCost penalty) {
    return new RideHailingAccessAdapter(this, penalty);
  }

  @Override
  public String toString() {
    return asString(true, false, null);
  }
}
