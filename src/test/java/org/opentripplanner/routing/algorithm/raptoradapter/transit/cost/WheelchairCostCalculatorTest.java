package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityFeature;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;

public class WheelchairCostCalculatorTest {

  final int UNKNOWN_ACCESSIBILITY_COST = 500;
  final int INACCESSIBLE_TRIP_COST = 10000;

  private final WheelchairCostCalculator subject = new WheelchairCostCalculator(
    new DummyCostCalculator(),
    new WheelchairAccessibilityRequest(
      true,
      new WheelchairAccessibilityFeature(false, UNKNOWN_ACCESSIBILITY_COST, INACCESSIBLE_TRIP_COST),
      new WheelchairAccessibilityFeature(false, UNKNOWN_ACCESSIBILITY_COST, INACCESSIBLE_TRIP_COST)
    )
  );

  @Test
  public void boardAccessibleTrip() {
    var schedule = new TestTripSchedule(WheelChairBoarding.POSSIBLE);
    assertEquals(DummyCostCalculator.COST, board(schedule));
  }

  @Test
  public void boardTripWithUnknownAccessibility() {
    var schedule = new TestTripSchedule(WheelChairBoarding.NO_INFORMATION);
    assertEquals(DummyCostCalculator.COST + UNKNOWN_ACCESSIBILITY_COST * 100, board(schedule));
  }

  @Test
  public void boardInaccessibleTrip() {
    var schedule = new TestTripSchedule(WheelChairBoarding.NOT_POSSIBLE);
    assertEquals(DummyCostCalculator.COST + (INACCESSIBLE_TRIP_COST * 100), board(schedule));
  }

  private int board(TestTripSchedule schedule) {
    return subject.boardingCost(
      true,
      0,
      5,
      100,
      schedule,
      RaptorTransferConstraint.REGULAR_TRANSFER
    );
  }
}
