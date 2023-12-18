package org.opentripplanner.raptor.spi;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * The responsibility is to calculate multi-criteria value (like the generalized cost).
 * <p/>
 * The implementation should be immutable and thread safe.
 */
public interface RaptorCostCalculator<T extends RaptorTripSchedule> {
  /**
   * The cost is zero (0) if it's not calculated or if the cost "element" have no cost associated.
   * with it.
   */
  int ZERO_COST = 0;

  /**
   * Calculate cost when on-board of a trip. The cost is only used to compare to paths on the same
   * trip - so any cost that is constant for a given trip can be dropped, but it will make debugging
   * easier if the cost can be compared with the "stop-arrival-cost". The cost must incorporate the
   * fact that 2 boarding may happen at 2 different stops.
   */
  int boardingCost(
    boolean firstBoarding,
    int prevArrivalTime,
    int boardStop,
    int boardTime,
    T trip,
    RaptorTransferConstraint transferConstraints
  );

  /**
   * Calculate cost of boarding a trip. This should be the cost of the waiting time, any board and
   * transfer cost, and the penalty for the board stop visit. This cost should NOT include the
   * previous stop arrival cost, but the incremental cost to be added to the previous stop arrival
   * cost.
   */
  int onTripRelativeRidingCost(int boardTime, T tripScheduledBoarded);

  /**
   * Calculate the value when arriving by transit.
   */
  int transitArrivalCost(int boardCost, int alightSlack, int transitTime, T trip, int toStop);

  /**
   * Calculate the value, when waiting between the last transit and egress paths
   */
  int waitCost(int waitTimeInSeconds);

  /**
   * Used for estimating the remaining value for a criteria at a given stop arrival. The calculated
   * value should be a an optimistic estimate for the heuristics to work properly. So, to calculate
   * the generalized cost for given the {@code minTravelTime} and {@code minNumTransfers} retuning
   * the greatest value, which is guaranteed to be less than the
   * <em>real value</em> would be correct and a good choose.
   */
  int calculateMinCost(int minTravelTime, int minNumTransfers);

  /**
   * This method allows the cost calculator to add cost in addition to the generalized-cost of the
   * given egress itself. For example you might want to add a transfer cost to FLEX egress.
   *
   * @return the {@link RaptorTransfer#c1()} plus any additional board or transfer
   * cost.
   */
  int costEgress(RaptorAccessEgress egress);
}
