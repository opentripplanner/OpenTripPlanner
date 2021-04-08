package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public class AccessEgress implements RaptorTransfer {

  /**
   * "To stop" in the case of access, "from stop" in the case of egress.
   */
  private int toFromStop;

  private final int durationInSeconds;

  /**
   * This should be the last state both in the case of access and egress.
   */
  private final State lastState;

  public AccessEgress(int toFromStop, int durationInSeconds, State lastState) {
    this.toFromStop = toFromStop;
    this.durationInSeconds = durationInSeconds;
    this.lastState = lastState;
  }

  @Override
  public int stop() {
    return toFromStop;
  }

  @Override
  public int durationInSeconds() {
    return durationInSeconds;
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