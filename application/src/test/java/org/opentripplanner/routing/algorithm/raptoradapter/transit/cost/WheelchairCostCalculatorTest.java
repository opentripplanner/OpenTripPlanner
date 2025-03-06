package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.transit.model.basic.Accessibility;

public class WheelchairCostCalculatorTest {

  static final int UNKNOWN_ACCESSIBILITY_COST = 500;
  static final int INACCESSIBLE_TRIP_COST = 10000;
  private static final int BOARD_COST_SEC = 5;
  private static final int TRANSFER_COST_SEC = 2;
  private static final double WAIT_RELUCTANCE_FACTOR = 0.5;

  private final DefaultCostCalculator<TestTripSchedule> defaultCostCalculator =
    new DefaultCostCalculator<>(
      BOARD_COST_SEC,
      TRANSFER_COST_SEC,
      WAIT_RELUCTANCE_FACTOR,
      null,
      null
    );

  private final WheelchairCostCalculator<TestTripSchedule> wheelchairCostCalculator =
    new WheelchairCostCalculator<>(
      defaultCostCalculator,
      AccessibilityPreferences.ofCost(UNKNOWN_ACCESSIBILITY_COST, INACCESSIBLE_TRIP_COST)
    );
  private final TestTripSchedule.Builder scheduleBuilder = TestTripSchedule.schedule("12:00 12:01");

  static Stream<Arguments> testCases() {
    return Stream.of(
      Arguments.of(Accessibility.POSSIBLE, 0),
      Arguments.of(Accessibility.NO_INFORMATION, UNKNOWN_ACCESSIBILITY_COST),
      Arguments.of(Accessibility.NOT_POSSIBLE, INACCESSIBLE_TRIP_COST)
    );
  }

  @ParameterizedTest(name = "accessibility of {0} should add an extra cost of {1}")
  @MethodSource("testCases")
  public void calculateExtraBoardingCost(Accessibility wcb, int expectedExtraCost) {
    var schedule = scheduleBuilder.wheelchairBoarding(wcb).build();

    int defaultCost = calculateBoardingCost(schedule, defaultCostCalculator);

    int wheelchairBoardCost = calculateBoardingCost(schedule, wheelchairCostCalculator);
    int expected = defaultCost + RaptorCostConverter.toRaptorCost(expectedExtraCost);

    assertEquals(expected, wheelchairBoardCost);
  }

  private int calculateBoardingCost(
    TestTripSchedule schedule,
    RaptorCostCalculator<TestTripSchedule> calc
  ) {
    return calc.boardingCost(true, 0, 5, 100, schedule, RaptorTransferConstraint.REGULAR_TRANSFER);
  }
}
