package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import org.opentripplanner.transit.raptor.api.transit.CostCalculator;

public class CostCalculatorFactory {

  public static CostCalculator createCostCalculator(
    McCostParams mcCostParams,
    int[] stopBoardAlightCosts
  ) {
    CostCalculator calculator = new DefaultCostCalculator(mcCostParams, stopBoardAlightCosts);

    if (mcCostParams.accessibilityRequirements().enabled()) {
      calculator =
        new WheelchairCostCalculator(calculator, mcCostParams.accessibilityRequirements());
    }

    return calculator;
  }
}
