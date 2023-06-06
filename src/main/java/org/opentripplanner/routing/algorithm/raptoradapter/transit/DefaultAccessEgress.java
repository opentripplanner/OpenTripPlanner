package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;
import org.opentripplanner.street.search.state.State;

/**
 * Default implementation of the RaptorAccessEgress interface.
 */
public class DefaultAccessEgress implements RaptorAccessEgress {

  private final int stop;
  private final int durationInSeconds;
  private final int generalizedCost;

  /**
   * This should be the last state both in the case of access and egress.
   */
  private final State lastState;

  public DefaultAccessEgress(int stop, State lastState) {
    this.stop = stop;
    this.durationInSeconds = (int) lastState.getElapsedTimeSeconds();
    this.generalizedCost = RaptorCostConverter.toRaptorCost(lastState.getWeight());
    this.lastState = lastState;
  }

  @Override
  public boolean hasOpeningHours() {
    return false;
  }

  @Override
  public int durationInSeconds() {
    return durationInSeconds;
  }

  @Override
  public int stop() {
    return stop;
  }

  @Override
  public int generalizedCost() {
    return generalizedCost;
  }

  public State getLastState() {
    return lastState;
  }

  @Override
  public String toString() {
    return asString(true) + (lastState != null ? " (" + lastState + ")" : "");
  }

  public boolean isWalkOnly() {
    return lastState.containsOnlyWalkMode();
  }
}
