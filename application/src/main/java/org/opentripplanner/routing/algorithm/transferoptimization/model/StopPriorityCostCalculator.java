package org.opentripplanner.routing.algorithm.transferoptimization.model;

import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * This class calculates an extra stop priority cost by using the stop-board-alight-transfer-cost
 * and boosting it by multiplying it with a {@code factor}.
 */
public class StopPriorityCostCalculator {

  /**
   * @see RaptorTransitData#getStopBoardAlightTransferCosts()
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
