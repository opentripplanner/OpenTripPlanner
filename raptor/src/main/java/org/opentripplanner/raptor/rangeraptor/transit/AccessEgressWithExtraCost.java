package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.raptor.api.model.AbstractAccessEgressDecorator;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;

/**
 * This decorator will add the an extra cost factor to the c1 of the access/egress.
 */
public class AccessEgressWithExtraCost extends AbstractAccessEgressDecorator {

  private final double costFactor;

  public AccessEgressWithExtraCost(RaptorAccessEgress delegate, double costFactor) {
    super(delegate);
    this.costFactor = costFactor;
  }

  @Override
  public int c1() {
    return (int) (delegate().c1() * costFactor);
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
