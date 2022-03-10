package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public class AccessEgress implements RaptorTransfer {

  /**
   * "To stop" in the case of access, "from stop" in the case of egress.
   */
  private int toFromStop;

  private final int durationInSeconds;

  private final int generalizedCost;

  /**
   * This should be the last state both in the case of access and egress.
   */
  private final State lastState;

  public AccessEgress(int toFromStop, State lastState) {
    this.toFromStop = toFromStop;
    this.durationInSeconds = (int) lastState.getElapsedTimeSeconds();
    this.generalizedCost = RaptorCostConverter.toRaptorCost(lastState.getWeight());
    this.lastState = lastState;
  }

  @Override
  public int stop() {
    return toFromStop;
  }

  @Override
  public int generalizedCost() {
    return generalizedCost;
  }

  @Override
  public int durationInSeconds() {
    return durationInSeconds;
  }

  @Override
  public boolean hasOpeningHours() { return false; }

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