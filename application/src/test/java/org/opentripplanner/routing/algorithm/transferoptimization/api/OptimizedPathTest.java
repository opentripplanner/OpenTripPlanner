package org.opentripplanner.routing.algorithm.transferoptimization.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.raptor.api.model.RaptorValueType.C1;
import static org.opentripplanner.raptor.api.model.RaptorValueType.C2;
import static org.opentripplanner.raptor.api.model.RaptorValueType.TRANSFER_PRIORITY;
import static org.opentripplanner.raptor.api.model.RaptorValueType.WAIT_TIME_COST;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.routing.algorithm.transferoptimization.BasicPathTestCase;

class OptimizedPathTest implements RaptorTestConstants {

  @Test
  void copyBasicPath() {
    // Given: a wrapped basic path
    var path = new OptimizedPath<>(BasicPathTestCase.basicTripAsPath());

    // Verify all costs
    assertEquals(BasicPathTestCase.TOTAL_C1, path.c1());
    assertEquals(0, path.breakTieCost());
    assertEquals(BasicPathTestCase.TOTAL_C1, path.generalizedCostWaitTimeOptimized());
    assertEquals(66_00, path.transferPriorityCost());

    // And toString is the same (transfer priority cost added)
    assertEquals(
      BasicPathTestCase.BASIC_PATH_AS_STRING.replace("]", " Tₚ6_600]"),
      path.toString(this::stopIndexToName)
    );

    // Verify details
    assertEquals(
      BasicPathTestCase.BASIC_PATH_AS_DETAILED_STRING.replace("]", " Tₚ6_600]"),
      path.toStringDetailed(this::stopIndexToName)
    );

    // Make sure the toString does not throw an exception or return null
    assertNotNull(path.toString());
  }

  @Test
  void copyBasicPathWithCostsAndVerifyCosts() {
    var orgPath = BasicPathTestCase.basicTripAsPath();
    var accessLeg = orgPath.accessLeg();

    // Define some constants
    final int generalizedCost = 881100;
    final int c2 = 7;
    final int transferPriorityCost = 120100;
    final int waitTimeOptimizedCost = 130200;
    final int generalizedCostWaitTimeOptimized = generalizedCost + waitTimeOptimizedCost;
    final int breakTieCost = 140300;

    var path = new OptimizedPath<>(
      accessLeg,
      orgPath.rangeRaptorIterationDepartureTime(),
      generalizedCost,
      c2,
      transferPriorityCost,
      waitTimeOptimizedCost,
      breakTieCost
    );

    assertEquals(generalizedCost, path.c1());
    assertEquals(breakTieCost, path.breakTieCost());
    assertEquals(generalizedCostWaitTimeOptimized, path.generalizedCostWaitTimeOptimized());
    assertEquals(transferPriorityCost, path.transferPriorityCost());

    var exp = BasicPathTestCase.BASIC_PATH_AS_STRING.replace(
      "C₁8_154 C₂7]",
      C1.format(generalizedCost) +
        " " +
        C2.format(c2) +
        " " +
        TRANSFER_PRIORITY.format(transferPriorityCost) +
        " " +
        WAIT_TIME_COST.format(generalizedCostWaitTimeOptimized) +
        "]"
    );

    assertEquals(exp, path.toString(this::stopIndexToName));
  }
}
// 881 100 + 130 200 = 1 011 300
