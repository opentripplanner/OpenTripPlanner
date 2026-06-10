package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.Objects;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.street.search.state.State;

/**
 * Default implementation of the RaptorAccessEgress interface.
 * <p>
 * Implementation note: As stated in the RoutingAccessEgress interface contract {@link RoutingAccessEgress#getFinalState()},
 * this class exposes the final A* state in search order, not in chronological order. For egress searches this State is
 * unreversed ({@code request.arriveBy() == true}) — reversal is deferred to
 * {@link org.opentripplanner.astar.model.GraphPath} construction, which only happens for
 * winning paths during itinerary mapping. This avoids the cost of cloning and reversing the entire State
 * chain for every egress candidate.
 * <p>
 * The scalar values extracted below ({@code getElapsedTimeSeconds}, {@code getWeight},
 * {@code containsOnlyWalkMode}) are direction-independent and produce identical results on
 * both reversed and unreversed State chains.
 */
public class DefaultAccessEgress implements RoutingAccessEgress {

  private final int stop;
  private final int durationInSeconds;
  private final int generalizedCost;
  private final int timePenalty;

  /** Keep this to be able to map back to itinerary */
  private final TimeAndCost penalty;

  private final State finalState;

  /**
   * This is public to allow unit-tests full control over the field values.
   */
  public DefaultAccessEgress(
    int stop,
    int durationInSeconds,
    int generalizedCost,
    TimeAndCost penalty,
    State finalState
  ) {
    this.stop = stop;
    this.durationInSeconds = durationInSeconds;
    this.generalizedCost = generalizedCost;
    this.timePenalty = penalty.isZero() ? RaptorConstants.TIME_NOT_SET : penalty.timeInSeconds();
    this.penalty = penalty;
    this.finalState = Objects.requireNonNull(finalState);
  }

  public DefaultAccessEgress(int stop, State finalState) {
    this(
      stop,
      (int) finalState.getElapsedTimeSeconds(),
      RaptorCostConverter.toRaptorCost(finalState.getWeight()),
      TimeAndCost.ZERO,
      finalState
    );
  }

  protected DefaultAccessEgress(RoutingAccessEgress other, TimeAndCost penalty) {
    // In the API we have a cost associated with the time-penalty. In Raptor, there is no
    // association between the time-penalty and the cost. So, we add the time-penalty cost to
    // the generalized cost here. In logic later on, we will remove it.
    this(
      other.stop(),
      other.durationInSeconds(),
      other.c1() + penalty.cost().toCentiSeconds(),
      penalty,
      other.getFinalState()
    );
    if (other.penalty() != TimeAndCost.ZERO) {
      throw new IllegalStateException("Can not add penalty twice...");
    }
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

  /**
   * The final state from the access/egress street search. For egress searches this State is
   * unreversed ({@code request.arriveBy() == true}) — reversal is deferred to
   * {@link org.opentripplanner.astar.model.GraphPath} construction, which only happens for
   * winning paths during itinerary mapping. This avoids the cost of cloning and reversing the entire State
   * chain for every egress candidate.
   * <p>
   * The scalar values extracted below ({@code getElapsedTimeSeconds}, {@code getWeight},
   * {@code containsOnlyWalkMode}) are direction-independent and produce identical results on
   * both reversed and unreversed State chains.
   */
  @Override
  public State getFinalState() {
    return finalState;
  }

  @Override
  public boolean isWalkOnly() {
    return finalState.containsOnlyWalkMode();
  }

  @Override
  public TimeAndCost penalty() {
    return penalty;
  }

  /**
   * Return a new copy of this with the requested penalty.
   * <p>
   * OVERRIDE THIS IF KEEPING THE TYPE IS IMPORTANT!
   */
  @Override
  public RoutingAccessEgress withPenalty(TimeAndCost penalty) {
    return new DefaultAccessEgress(this, penalty);
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
    if (!(o instanceof RoutingAccessEgress that)) {
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
