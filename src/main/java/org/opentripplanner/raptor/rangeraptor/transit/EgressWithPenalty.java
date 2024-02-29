package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.raptor.api.model.AbstractAccessEgressDecorator;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;

/**
 * This decorator will add the time penalty to the duration of the egress and adjust the
 * `requestedDepartureTime` when time-shifting the egress according to opening-hours.
 * <p>
 * @see AccessWithPenalty for more info on time-penalty.
 */
public class EgressWithPenalty extends AbstractAccessEgressDecorator {

  public EgressWithPenalty(RaptorAccessEgress delegate) {
    super(delegate);
  }

  @Override
  public int durationInSeconds() {
    return delegate().durationInSeconds() + delegate().timePenalty();
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    int dt = delegate().timePenalty();
    int adjustedTime = delegate().latestArrivalTime(requestedArrivalTime - dt);
    return adjustedTime == RaptorConstants.TIME_NOT_SET
      ? RaptorConstants.TIME_NOT_SET
      : adjustedTime + dt;
  }

  /**
   * This class is used internally in Raptor to decorate an access path. This method removes the
   * decorator and returns the original access path if decorated. If not, the given path is
   * returned.
   */
  public RaptorAccessEgress removeDecorator() {
    return delegate();
  }
}
