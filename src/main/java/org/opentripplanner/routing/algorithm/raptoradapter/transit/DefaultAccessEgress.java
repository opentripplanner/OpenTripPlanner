package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.Objects;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;
import org.opentripplanner.street.search.state.State;

/**
 * Default implementation of the RaptorAccessEgress interface.
 */
public class DefaultAccessEgress implements RaptorAccessEgress {

  private final int stop;
  private final int durationInSeconds;
  private final int generalizedCost;
  private final int timePenalty;

  /** Keep this to be able to map back to itinerary */
  private final TimeAndCost penalty;

  /**
   * This should be the last state both in the case of access and egress.
   */
  private final State lastState;

  public DefaultAccessEgress(int stop, State lastState) {
    this.stop = stop;
    this.durationInSeconds = (int) lastState.getElapsedTimeSeconds();
    this.generalizedCost = RaptorCostConverter.toRaptorCost(lastState.getWeight());
    this.lastState = lastState;
    this.timePenalty = RaptorConstants.TIME_NOT_SET;
    this.penalty = TimeAndCost.ZERO;
  }

  protected DefaultAccessEgress(DefaultAccessEgress other, TimeAndCost penalty) {
    if (other.hasPenalty()) {
      throw new IllegalStateException("Can not add penalty twice...");
    }
    this.stop = other.stop();
    this.durationInSeconds = other.durationInSeconds();
    // In the API we have a cost associated with the time-penalty. In Raptor, there is no
    // association between the time-penalty and the cost. So, we add the time-penalty cost to
    // the generalized cost here. In logic later on, we will remove it.
    this.generalizedCost = other.c1() + penalty.cost().toCentiSeconds();
    this.timePenalty = penalty.isZero() ? RaptorConstants.TIME_NOT_SET : penalty.timeInSeconds();
    this.penalty = penalty;
    this.lastState = other.getLastState();
  }

  /**
   * Return a new copy of this with the requested penalty.
   * <p>
   * OVERRIDE THIS IF KEEPING THE TYPE IS IMPORTANT!
   */
  public DefaultAccessEgress withPenalty(TimeAndCost penalty) {
    return new DefaultAccessEgress(this, penalty);
  }

  @Override
  public int durationInSeconds() {
    return durationInSeconds;
  }

  @Override
  public int timePenalty() {
    return timePenalty;
  }

  @Override
  public int stop() {
    return stop;
  }

  @Override
  public int c1() {
    return generalizedCost;
  }

  @Override
  public boolean hasOpeningHours() {
    return false;
  }

  public State getLastState() {
    return lastState;
  }

  public boolean isWalkOnly() {
    return lastState.containsOnlyWalkMode();
  }

  public boolean hasPenalty() {
    return !penalty.isZero();
  }

  public TimeAndCost penalty() {
    return penalty;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    return requestedDepartureTime;
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    return requestedArrivalTime;
  }

  @Override
  public String toString() {
    return asString(true, true, summary());
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    // We check the contract of DefaultAccessEgress used for routing for equality, we do not care
    // if the entries are different implementation or have different AStar paths(lastState).
    if (!(o instanceof DefaultAccessEgress that)) {
      return false;
    }
    return (
      stop() == that.stop() &&
      durationInSeconds() == that.durationInSeconds() &&
      c1() == that.c1() &&
      penalty().equals(that.penalty())
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(stop, durationInSeconds, generalizedCost, penalty);
  }

  /**
   * Include summary information in toString. We only include information relevant for using this
   * in routing (not latestState).
   */
  private String summary() {
    return penalty.isZero() ? null : "w/penalty" + penalty;
  }
}
