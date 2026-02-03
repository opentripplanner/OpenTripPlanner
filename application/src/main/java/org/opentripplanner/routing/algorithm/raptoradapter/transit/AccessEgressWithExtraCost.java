package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import org.opentripplanner.raptor.api.model.AbstractAccessEgressDecorator;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;

/**
 * This decorator will add an extra cost factor to the c1 of the access/egress.
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
}
