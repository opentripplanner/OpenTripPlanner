package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.transit.raptor.api.transit.AbstractRaptorTransfer;
import org.opentripplanner.util.lang.ToStringBuilder;

public class AccessEgress extends AbstractRaptorTransfer {

  /**
   * This should be the last state both in the case of access and egress.
   */
  private final State lastState;

  public AccessEgress(int toFromStop, State lastState) {
    super(
      toFromStop,
      (int) lastState.getElapsedTimeSeconds(),
      RaptorCostConverter.toRaptorCost(lastState.getWeight())
    );
    this.lastState = lastState;
  }

  @Override
  public boolean hasOpeningHours() {
    return false;
  }

  public State getLastState() {
    return lastState;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(AccessEgress.class)
      .addStr("transfer", asString())
      .addObj("state", lastState)
      .toString();
  }
}
