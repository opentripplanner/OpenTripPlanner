package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor._data.transit.TestTransfer;

public class DefaultCostCalculatorTest {

  private static final int BOARD_COST_SEC = 5;
  private static final int TRANSFER_COST_SEC = 2;
  private static final double WAIT_RELUCTANCE_FACTOR = 0.5;
  private static final double TRANSIT_RELUCTANCE_FACTOR_1 = 1.0;
  private static final double TRANSIT_RELUCTANCE_FACTOR_2 = 0.8;
  private static final int TRANSIT_RELUCTANCE_1 = 0;
  private static final int TRANSIT_RELUCTANCE_2 = 1;
  private static final int STOP_A = 0;
  private static final int STOP_B = 1;

  private final DefaultCostCalculator subject = new DefaultCostCalculator(
    BOARD_COST_SEC,
    TRANSFER_COST_SEC,
    WAIT_RELUCTANCE_FACTOR,
    new double[] { TRANSIT_RELUCTANCE_FACTOR_1, TRANSIT_RELUCTANCE_FACTOR_2 },
    new int[] { 0, 25 }
  );

  @Test
  public void boardCostRegularTransfer() {
    int time0 = 10;
    int oneSec = time0 + 1;

    assertEquals(500, subject.boardingCostRegularTransfer(true, time0, STOP_A, time0));
    assertEquals(500, subject.boardingCostRegularTransfer(true, time0, STOP_B, time0));
    assertEquals(550, subject.boardingCostRegularTransfer(true, time0, STOP_A, oneSec));
    assertEquals(700, subject.boardingCostRegularTransfer(false, time0, STOP_A, time0));
    assertEquals(725, subject.boardingCostRegularTransfer(false, time0, STOP_B, time0));
    assertEquals(750, subject.boardingCostRegularTransfer(false, time0, STOP_A, oneSec));
  }

  @Test
  public void transitArrivalCost() {
    assertEquals(0, subject.transitArrivalCost(0, 0, 0, TRANSIT_RELUCTANCE_1, STOP_A), "Zero cost");
    assertEquals(
      1000,
      subject.transitArrivalCost(1000, 0, 0, TRANSIT_RELUCTANCE_1, STOP_A),
      "Board cost"
    );
    assertEquals(
      50,
      subject.transitArrivalCost(0, 1, 0, TRANSIT_RELUCTANCE_1, STOP_A),
      "Alight wait time cost"
    );
    assertEquals(
      100,
      subject.transitArrivalCost(0, 0, 1, TRANSIT_RELUCTANCE_1, STOP_A),
      "Transit time cost"
    );
    assertEquals(
      25,
      subject.transitArrivalCost(0, 0, 0, TRANSIT_RELUCTANCE_1, STOP_B),
      "Alight stop cost"
    );
    assertEquals(
      1175,
      subject.transitArrivalCost(1000, 1, 1, TRANSIT_RELUCTANCE_1, STOP_B),
      "Total cost"
    );
  }

  @Test
  public void onTripRidingCost() {
    assertEquals(0, subject.onTripRelativeRidingCost(0, TRANSIT_RELUCTANCE_1), "Board cost");
    assertEquals(0, subject.onTripRelativeRidingCost(0, TRANSIT_RELUCTANCE_2), "Board cost");
    assertEquals(-100, subject.onTripRelativeRidingCost(1, TRANSIT_RELUCTANCE_1), "Board cost");
    assertEquals(-80, subject.onTripRelativeRidingCost(1, TRANSIT_RELUCTANCE_2), "Board cost");
  }

  @Test
  public void calculateMinCost() {
    // Given:
    //   - Board cost:     500
    //   - Transfer cost:  200
    //   - Transit factor:  80 (min of 80 and 100)

    // Board cost is 500:
    assertEquals(500, subject.calculateMinCost(0, 0));
    // The transfer 1s * 80 = 80 + board cost 500
    assertEquals(580, subject.calculateMinCost(1, 0));
    // Board 2 times and transfer 1: 2 * 500 + 200
    assertEquals(1200, subject.calculateMinCost(0, 1));

    // Transit 200s * 80 + Board 4 * 500 + Transfer 3 * 200
    assertEquals(18_600, subject.calculateMinCost(200, 3));
  }

  @Test
  public void testConvertBetweenRaptorAndMainOtpDomainModel() {
    assertEquals(RaptorCostConverter.toRaptorCost(BOARD_COST_SEC), subject.calculateMinCost(0, 0));
    assertEquals(
      RaptorCostConverter.toRaptorCost(0.8 * 20 + BOARD_COST_SEC),
      subject.calculateMinCost(20, 0)
    );
  }

  @Test
  public void testCostEgressWithoutRides() {
    // Should be transfer generalized cost minus stop transfer cost

    var GENERALIZED_COST = 100;

    // transfer cost on stop index 0 is 0 - do not subtract anything
    var t1 = TestTransfer.walk(0, 15, GENERALIZED_COST);
    assertEquals(GENERALIZED_COST, subject.costEgress(t1));
    // transfer cost on stop index 1 is 25 - subtract 25 from generalized cost
    var t2 = TestTransfer.walk(1, 15, 100);
    assertEquals(GENERALIZED_COST - 25, subject.costEgress(t2));
  }

  @Test
  public void testCostEgressWithRides() {
    // Should be generalized cost plus transfer cost

    var GENERALIZED_COST = 100;
    var DESIRED_COST = GENERALIZED_COST + TRANSFER_COST_SEC * 100;

    // Should be the same on all stop indexes
    var t1 = TestTransfer.flex(0, 15, 1, GENERALIZED_COST);
    assertEquals(DESIRED_COST, subject.costEgress(t1));
    var t2 = TestTransfer.flex(1, 15, 1, GENERALIZED_COST);
    assertEquals(DESIRED_COST, subject.costEgress(t2));
  }
}
