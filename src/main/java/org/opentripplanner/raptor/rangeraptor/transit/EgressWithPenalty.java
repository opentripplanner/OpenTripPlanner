package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.raptor.api.model.AbstractAccessEgressDecorator;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;

/**
 * This decorator will add the time penalty to the duration of the egress and adjust the
 * `requestedDepartureTime` when time-shifting the egress according to opening-hours.
 *
 * TODO PEN - Write more
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
    return delegate().latestArrivalTime(requestedArrivalTime - delegate().timePenalty());
  }

  /**
   * This class is used internally in Raptor to decorate an access path. This method removes the
   * decorator and returns the original access path if decorated. If not, the given path is
   * returned.
   */
  public static RaptorAccessEgress removeDecoratorIfItExist(RaptorAccessEgress path) {
    return removeRaptorDecoratorIfItExist(path, EgressWithPenalty.class);
  }
}
