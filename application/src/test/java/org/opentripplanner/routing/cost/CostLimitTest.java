package org.opentripplanner.routing.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorCostConverter;

class CostLimitTest {

  private static final int MAX_RAPTOR_COST = RaptorCostConverter.toRaptorCost(
    (double) CostLimit.MAX_COST
  );

  @Test
  void costBelowLimitIsConvertedUnchanged() {
    // A normal access/egress/transfer cost is converted exactly like the plain converter.
    assertEquals(RaptorCostConverter.toRaptorCost(123.45), CostLimit.toRaptorCost(123.45));
    assertEquals(RaptorCostConverter.toRaptorCost(0.0), CostLimit.toRaptorCost(0.0));
  }

  @Test
  void costAtLimitIsConvertedUnchanged() {
    assertEquals(MAX_RAPTOR_COST, CostLimit.toRaptorCost(CostLimit.MAX_COST));
  }

  @Test
  void costAboveLimitIsCapped() {
    assertEquals(MAX_RAPTOR_COST, CostLimit.toRaptorCost(CostLimit.MAX_COST + 1.0));
  }

  @Test
  void negativeCostIsCappedNotTreatedAsFree() {
    // A negative input means the cost already overflowed upstream; treat it as the worst case.
    assertEquals(MAX_RAPTOR_COST, CostLimit.toRaptorCost(-5.0));
  }

  @Test
  void wholeSecondsVariantTruncatesSubSecondCost() {
    // Transfers historically truncate the cost to whole transit-seconds before converting;
    // toRaptorCostWholeSeconds preserves that, while toRaptorCost keeps centi-second precision.
    assertEquals(12_300, CostLimit.toRaptorCostWholeSeconds(123.99));
    assertEquals(12_399, CostLimit.toRaptorCost(123.99));
  }

  @Test
  void wholeSecondsVariantCapsLargeAndNegativeCosts() {
    assertEquals(MAX_RAPTOR_COST, CostLimit.toRaptorCostWholeSeconds(22_778_951.0));
    assertEquals(MAX_RAPTOR_COST, CostLimit.toRaptorCostWholeSeconds(-5.0));
  }

  @Test
  void cappedCostLeavesHeadroomForSummingSeveralLegs() {
    // access + egress + a transfer, all capped, must still be well below the int sentinels so the
    // path cost sum cannot overflow.
    long threeCappedLegs = 3L * MAX_RAPTOR_COST;
    assertTrue(threeCappedLegs < RaptorConstants.UNREACHED_HIGH);
  }
}
