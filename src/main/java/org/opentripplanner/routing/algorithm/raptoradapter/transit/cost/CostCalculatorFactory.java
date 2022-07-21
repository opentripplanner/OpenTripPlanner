package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import org.opentripplanner.transit.raptor.api.transit.CostCalculator;

public class CostCalculatorFactory {

  public static <T extends DefaultTripSchedule> CostCalculator<T> createCostCalculator(
    McCostParams mcCostParams,
    int[] stopBoardAlightCosts
  ) {
    CostCalculator<T> calculator = new DefaultCostCalculator<>(mcCostParams, stopBoardAlightCosts);

    if (mcCostParams.accessibilityRequirements().enabled()) {
      calculator =
        new WheelchairCostCalculator<>(calculator, mcCostParams.accessibilityRequirements());
    }

    // append RouteCostCalculator to calculator stack if (un)preferred routes exist
    if (!mcCostParams.unpreferredRoutes().isEmpty()) {
      calculator =
        new RouteCostCalculator(
          calculator,
          mcCostParams.unpreferredRoutes(),
          mcCostParams.unnpreferredCost()
        );
    }

    return calculator;
  }
}
