package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.raptor.api.model.AbstractAccessEgressDecorator;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;

/**
 * This decorator will add the time penalty to the duration of the access and adjust the
 * `requestedDepartureTime` when time-shifting the access according to opening-hours.
 *
 * TODO PEN - Write more
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
    return delegate().earliestDepartureTime(requestedDepartureTime + delegate().timePenalty());
  }
}
