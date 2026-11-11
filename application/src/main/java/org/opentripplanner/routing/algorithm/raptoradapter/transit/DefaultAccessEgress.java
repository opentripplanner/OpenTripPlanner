package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.Objects;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.routing.cost.CostLimit;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.search.state.State;

/**
 * Default implementation of the RaptorAccessEgress interface.
 * <p>
 * Implementation note: This class keeps the final A* state in search order, not in chronological
 * order. For egress searches this State is unreversed ({@code request.arriveBy() == true}) —
 * reversal is deferred to {@link #streetPath()}, which is only called for winning paths during
 * itinerary mapping. This avoids the cost of cloning and reversing the entire State chain for
 * every egress candidate.
 * <p>
 * The scalar values extracted below ({@code getElapsedTimeSeconds}, {@code getWeight},
 * {@code containsOnlyWalkMode}) are direction-independent and produce identical results on both
 * reversed and unreversed State chains.
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
      CostLimit.toRaptorCost(finalState.getWeight()),
      TimeAndCost.ZERO,
      finalState
    );
  }

  /**
   * Create a new access/egress for the same stop and street search as {@code other}. Duration and
   * cost are derived from the street search again, so any penalty of {@code other} is dropped.
   */
  protected DefaultAccessEgress(DefaultAccessEgress other) {
    this(other.stop, other.finalState);
  }

  protected DefaultAccessEgress(DefaultAccessEgress other, TimeAndCost penalty) {
    // In the API we have a cost associated with the time-penalty. In Raptor, there is no
    // association between the time-penalty and the cost. So, we add the time-penalty cost to
    // the generalized cost here. In logic later on, we will remove it.
    this(
      other.stop(),
      other.durationInSeconds(),
      other.c1() + penalty.cost().toCentiSeconds(),
      penalty,
      other.finalState
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
   * The street path of this access/egress in chronological order. The path is built on every
   * call, so only call this for paths which are actually used, like when mapping an itinerary.
   */
  public StreetPath streetPath() {
    return new StreetPath(finalState);
  }

  @Override
  public boolean isRentingVehicleFromStation() {
    return finalState.isRentingVehicleFromStation();
  }

  /**
   * Return true if any part of the access/egress is traversed by car.
   */
  public boolean containsModeCar() {
    return finalState.containsModeCar();
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
   * Include summary information in toString. We only include information relevant for using this in
   * routing (not latestState).
   */
  private String summary() {
    return penalty.isZero() ? null : "w/penalty" + penalty;
  }
}
