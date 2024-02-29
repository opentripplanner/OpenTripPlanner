package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.raptor.api.model.AbstractAccessEgressDecorator;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;

/**
 * This decorator will add the time penalty to the duration of the access and adjust the
 * `requestedDepartureTime` when time-shifting the access according to opening-hours.
 * <p>
 * The time-penalty should be invisible outside the Raptor algorithm and should not be part of the
 * leg start and end times in the result. Inside Raptor the time-penalty is included in times used
 * for comparing arrivals (comparing paths for optimality). In some cases, we need to exclude the
 * time-penalty. Checking for limits like 'arrive-by'(in the forward search) and 'depart-after'(in
 * the reverse search) requires that the time is without the time-penalty. This class does not
 * do these checks.
 */
public class AccessWithPenalty extends AbstractAccessEgressDecorator {

  public AccessWithPenalty(RaptorAccessEgress delegate) {
    super(delegate);
  }

  @Override
  public int durationInSeconds() {
    return delegate().durationInSeconds() + delegate().timePenalty();
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    final int dt = delegate().timePenalty();
    int adjustedTime = delegate().earliestDepartureTime(requestedDepartureTime + dt);
    return adjustedTime == RaptorConstants.TIME_NOT_SET
      ? RaptorConstants.TIME_NOT_SET
      : adjustedTime - dt;
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
