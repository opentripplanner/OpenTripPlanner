package org.opentripplanner.routing.algorithm.transferoptimization.model;

import org.opentripplanner.framework.lang.IntUtils;

/**
 * This calculator is used to give the stop-priority-cost a boost, by multiplying it with a {@code
 * factor}.
 */
public class StopPriorityCostCalculator {

  private final int[] stopTransferCost;
  private final double extraStopTransferCostFactor;

  StopPriorityCostCalculator(double extraStopTransferCostFactor, int[] stopTransferCost) {
    this.stopTransferCost = stopTransferCost;
    this.extraStopTransferCostFactor = extraStopTransferCostFactor;
  }

  int extraStopPriorityCost(int stop) {
    return IntUtils.round(stopTransferCost[stop] * extraStopTransferCostFactor);
  }
}
