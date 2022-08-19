package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.function.DoubleFunction;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RequestFunctions;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.raptor._data.transit.TestTripPattern;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;

public class UnpreferredModeCostCalculatorTest {

  private static final int BOARD_COST_SEC = 5;
  private static final int TRANSFER_COST_SEC = 2;
  private static final double WAIT_RELUCTANCE_FACTOR = 0.5;
  private static final double TRANSIT_RELUCTANCE_FACTOR_1 = 1.0;
  private static final double TRANSIT_RELUCTANCE_FACTOR_2 = 0.8;
  private static final int STOP_A = 0;
  private static final int STOP_B = 1;
  private static final int UNPREFERRED_MODE_CONSTANT = 5;
  private static final double UNPREFERRED_MODE_COEFFICIENT = 0.3;

  private static final DoubleFunction<Double> unprefModeCost = RequestFunctions.createLinearFunction(
    UNPREFERRED_MODE_CONSTANT,
    UNPREFERRED_MODE_COEFFICIENT
  );

  private static final Set<TransitMode> unpreferredModes = Set.of(TransitMode.BUS);
  private final DefaultCostCalculator defaultCostCalculator = new DefaultCostCalculator(
    BOARD_COST_SEC,
    TRANSFER_COST_SEC,
    WAIT_RELUCTANCE_FACTOR,
    new double[] { TRANSIT_RELUCTANCE_FACTOR_1, TRANSIT_RELUCTANCE_FACTOR_2 },
    new int[] { 0, 25 }
  );

  private final UnpreferredModesCostCalculator subject = new UnpreferredModesCostCalculator(
    defaultCostCalculator,
    unpreferredModes,
    unprefModeCost
  );

  @Test
  public void transitArrivalCost() {
    TestTripSchedule trip = TestTripSchedule
      .schedule(TestTripPattern.pattern("L31", STOP_A, STOP_B))
      .arrivals("10:00 10:05")
      .departures("10:01 10:06")
      .build();

    assertEquals(
      RaptorCostConverter.toRaptorCost(UNPREFERRED_MODE_CONSTANT),
      subject.transitArrivalCost(0, 0, 0, trip, STOP_A),
      "Unpreferred constant penalty"
    );
    assertEquals(1800, subject.transitArrivalCost(0, 0, 10, trip, STOP_A), "Unpreferred mode cost");
  }

  @Test
  public void transitArrivalCost_notUnpreferred() {
    TestTripSchedule trip = TestTripSchedule
      .schedule(TestTripPattern.pattern("L31", STOP_A, STOP_B))
      .arrivals("10:00 10:05")
      .departures("10:01 10:06")
      .build();

    UnpreferredModesCostCalculator subject = new UnpreferredModesCostCalculator(
      defaultCostCalculator,
      Set.of(TransitMode.RAIL),
      unprefModeCost
    );

    assertEquals(0, subject.transitArrivalCost(0, 0, 0, trip, STOP_A), "Zero cost");
    assertEquals(1000, subject.transitArrivalCost(1000, 0, 0, trip, STOP_A), "Board cost");
    assertEquals(50, subject.transitArrivalCost(0, 1, 0, trip, STOP_A), "Alight wait time cost");
    assertEquals(100, subject.transitArrivalCost(0, 0, 1, trip, STOP_A), "Transit time cost");
    assertEquals(25, subject.transitArrivalCost(0, 0, 0, trip, STOP_B), "Alight stop cost");
    assertEquals(1175, subject.transitArrivalCost(1000, 1, 1, trip, STOP_B), "Total cost");
    assertEquals(50, subject.transitArrivalCost(0, 1, 0, trip, STOP_A), "Alight wait time cost");
    assertEquals(100, subject.transitArrivalCost(0, 0, 1, trip, STOP_A), "Transit time cost");
    assertEquals(25, subject.transitArrivalCost(0, 0, 0, trip, STOP_B), "Alight stop cost");
    assertEquals(1175, subject.transitArrivalCost(1000, 1, 1, trip, STOP_B), "Total cost");
  }
}
