package org.opentripplanner.routing.algorithm.transferoptimization.model;

import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;

/**
 * This class calculates an extra stop priority cost by using the stop-board-alight-transfer-cost
 * and boosting it by multiplying it with a {@code factor}.
 */
public class StopPriorityCostCalculator {

  /**
   * @see TransitLayer#getStopBoardAlightTransferCosts()
   */
  private final int[] stopBoardAlightTransferCosts;
  private final double extraStopBoardAlightCostFactor;

  StopPriorityCostCalculator(
    double extraStopBoardAlightCostFactor,
    int[] stopBoardAlightTransferCosts
  ) {
    this.stopBoardAlightTransferCosts = stopBoardAlightTransferCosts;
    this.extraStopBoardAlightCostFactor = extraStopBoardAlightCostFactor;
  }

  int extraStopPriorityCost(int stop) {
    return IntUtils.round(stopBoardAlightTransferCosts[stop] * extraStopBoardAlightCostFactor);
  }
}
