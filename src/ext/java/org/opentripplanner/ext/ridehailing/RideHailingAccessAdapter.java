package org.opentripplanner.ext.ridehailing;

import java.time.Duration;
import javax.annotation.Nullable;
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

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    return (int) (super.earliestDepartureTime(requestedDepartureTime) + arrival.toSeconds());
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    return (int) (super.latestArrivalTime(requestedArrivalTime) + arrival.toSeconds());
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

  @Nullable
  @Override
  public String openingHoursToString() {
    return "Arrival in " + arrival.toString();
  }

  @Override
  public String toString() {
    return asString(true);
  }
}
