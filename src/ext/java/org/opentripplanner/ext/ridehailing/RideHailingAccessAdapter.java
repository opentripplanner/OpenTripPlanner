package org.opentripplanner.ext.ridehailing;

import java.time.Duration;
import javax.annotation.Nullable;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.raptor.api.RaptorConstants;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;

/**
 * This class is used to adapt the ride hailing accesses (not egresses) into a time-dependent
 * multi-leg {@link DefaultAccessEgress}.
 * <p>
 * This adapter doesn't implement {@link org.opentripplanner.raptor.api.model.RaptorAccessEgress#latestArrivalTime(int)}
 * because we pretend that a ride hailing vehicle will be available at the time you finish the
 * transit leg.
 */
public class RideHailingAccessAdapter extends DefaultAccessEgress {

  private final Duration arrival;
  private final DefaultAccessEgress accessEgress;

  public RideHailingAccessAdapter(DefaultAccessEgress access, Duration arrival) {
    super(access.stop(), access.getLastState());
    this.arrival = arrival;
    this.accessEgress = access;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    int time = (int) (
      accessEgress.earliestDepartureTime(requestedDepartureTime) + arrival.toSeconds()
    );
    return mapToRaptorTime(time);
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

  private static int mapToRaptorTime(int time) {
    return time == StopTime.MISSING_VALUE ? RaptorConstants.TIME_NOT_SET : time;
  }
}
