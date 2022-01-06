package org.opentripplanner.routing.algorithm.transferoptimization.model;


/**
 * This calculator is used to give the stop-priority-cost a boost, by multiplying it
 * with a {@code factor}.
 */
public class StopPriorityCostCalculator {
    private final int[] stopVisitCost;
    private final double extraStopVisitCostFactor;

    StopPriorityCostCalculator(
            double extraStopVisitCostFactor,
            int[] stopVisitCost
    ) {
        this.stopVisitCost = stopVisitCost;
        this.extraStopVisitCostFactor = extraStopVisitCostFactor;
    }

    int extraStopPriorityCost(int stop) {
        return (int) Math.round((double) stopVisitCost[stop] * extraStopVisitCostFactor);
    }
}
