package org.opentripplanner.routing.algorithm.transferoptimization.api;

import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TransferWaitTimeCalculator;

/**
 * The transfer optimization is performed by calculating "cost" values:
 * <ol>
 *     <li>transfer-priority-cost</li>
 *     <li>wait-time-optimized-cost or generalized-cost</li>
 *     <li>break-tie-cost</li>
 * </ol>
 * <p>
 * If enabled all of these costs are used to find the optimal transfer-points for a given set of
 * transit legs. The transfer-priority takes precedence over the wait-time-optimized-cost, and the
 * break-tie-cost is only used if the cost is the same using the two other filters. For example for
 * a given path the normal case is that the the transfer-priority-cost is
 * {@link Transfer#NEUTRAL_PRIORITY_COST}. Then we look at the wait-time-optimized-cost or the
 * generalized-cost (from routing) - if this is tha same, then we use the break-tie-cost.
 * The wait-time-optimized-cost is typically the same when we can do a in-station/same-stop transfer
 * at multiple locations. The break-tie-cost just looks at the transit departure-times and try to
 * do the transfers as early as possible in the journey to minimize risk.
 * <p>
 * The wait-time-optimized-cost uses the generalized-cost as a baseline and adjust it to
 * optimize the wait-time. The cost of waiting is changed while the rest of the cost parts are
 * kept as is. If the wait-time-optimized-cost is not enabled, the generalized-cost from Raptor is
 * used instead.
 * <p>
 * This interface does not serve a role or implementation purpose, is is ONLY used to group and
 * describe the different costs for transfer optimizing. Two classes implement this interface.
 */
public interface TransferOptimized {

    int NEUTRAL_COST = 0;

    /**
     * Return the total transfer priority cost. This is completely separate from the
     * generalized cost. Return {@code 0}(zero) if cost is neutral/no "special" transfer
     * characteristics are present.
     * <p>
     * Precedence: first
     *
     * @see Transfer#priorityCost(Transfer)
     */
    int transferPriorityCost();

    /**
     * The generalized cost adjusted with a better wait time calculation.
     * <p>
     * Precedence: second
     *
     * @see TransferWaitTimeCalculator
     */
    int waitTimeOptimizedCost();

    /**
     * Optimize so that the transfers happens as early as possible. This is normally the case when
     * two trips visit the same stops - and two or more stops are possible transfer points.
     * <p>
     * Precedence: third
     */
    int breakTieCost();
}
