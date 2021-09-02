package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.WAIT_TIME_L11_L21;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.WAIT_TIME_L21_L31;
import static org.opentripplanner.util.time.DurationUtils.duration;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;

public class TransferWaitTimeCalculatorTest {
  private static final double WAIT_RELUCTANCE = 0.5;
  private static final double EPSILON = 0.1;

  final int d12s = duration("12s");
  final int d24s = duration("24s");
  final int d1m = duration("1m");
  final int d2m = duration("2m");
  final int d4m = duration("4m");
  final int d5m = duration("5m");
  final int d10m = duration("10m");
  final int d20m = duration("20m");
  final int d50m = duration("50m");
  final int d5d = 5 * duration("24h");

  private TransferWaitTimeCalculator subject;


  /**
   * verify initial condition with a few typical values:
   * <pre>
   *   f(0) = N * t0
   *   f(t0) = t0
   * </pre>
   */
  @Test
  public void calculateTxCostInitialConditions() {
    int zero = 0;
    int[] ts = { d1m, d5m, d50m };
    double[] ns = { 1.0, 2.0, 10.0 };

    for (double n : ns) {
      subject = new TransferWaitTimeCalculator(WAIT_RELUCTANCE,0.5, n);

      for (int t0 : ts) {
        subject.setMinSafeTransferTime(t0);
        String testCase = String.format("t0=%d, n=%.1f", t0, n);

        int costT0 = RaptorCostConverter.toRaptorCost(t0);

        assertEquals(n * costT0, subject.calculateOptimizedWaitCost(zero), EPSILON, "f(0) with " + testCase);
        assertEquals(
                costT0,
                subject.calculateOptimizedWaitCost(t0), EPSILON, "f(t0) with " + testCase
        );
      }
    }
  }

  @Test
  public void calculateTxCost_sample_A() {
    subject = new TransferWaitTimeCalculator(WAIT_RELUCTANCE, 1.0, 2.0);
    subject.setMinSafeTransferTime(d2m);

    assertEquals(23664.0, subject.calculateOptimizedWaitCost(1), EPSILON);
    assertEquals(20715.0, subject.calculateOptimizedWaitCost(d12s), EPSILON);
    assertEquals(18527.0, subject.calculateOptimizedWaitCost(d24s), EPSILON);
    assertEquals(14814.0, subject.calculateOptimizedWaitCost(d1m), EPSILON);
    assertEquals(9639.0, subject.calculateOptimizedWaitCost(d4m), EPSILON);
    assertEquals( 7360.0, subject.calculateOptimizedWaitCost(d10m), EPSILON);
    assertEquals( 2467.0, subject.calculateOptimizedWaitCost(d5d), EPSILON);
  }

  @Test
  public void calculateTxCost_sample_B() {
    subject = new TransferWaitTimeCalculator(WAIT_RELUCTANCE, 1.0, 5.0);
    subject.setMinSafeTransferTime(d10m);

    assertEquals(296607.0, subject.calculateOptimizedWaitCost(1), EPSILON);
    assertEquals(183569.0, subject.calculateOptimizedWaitCost(d1m), EPSILON);
    assertEquals(137515.0, subject.calculateOptimizedWaitCost(d2m), EPSILON);
    assertEquals(86196.0, subject.calculateOptimizedWaitCost(d5m), EPSILON);
    assertEquals(43106.0, subject.calculateOptimizedWaitCost(d20m), EPSILON);
    assertEquals(29870.0, subject.calculateOptimizedWaitCost(d50m), EPSILON);
    assertEquals(10174.0, subject.calculateOptimizedWaitCost(d5d), EPSILON);
  }

  @Test
  public void calculateTxCostWithNoMinSafeTxTimeThrowsException() {
    assertThrows(IllegalStateException.class, () -> {
      var subject = new TransferWaitTimeCalculator(WAIT_RELUCTANCE, 1.0, 2.0);
      subject.calculateOptimizedWaitCost(d20m);
    });
  }

  @Test
  public void testLinearCostPart() {
    // Given:
    var aPath = BasicPathTestCase.basicTripAsPath();
    double inverseWaitReluctance = 0.5;
    int waitTime = aPath.waitTime();

    // When: set min-safe-wait-time-factor to zero(0) and min-safe-transfer-time to any value
    var subject = new TransferWaitTimeCalculator(WAIT_RELUCTANCE, inverseWaitReluctance, 0.0);
    subject.setMinSafeTransferTime(99999999);

    // Then: expect the cost to be equal to the original cost minus cost of waiting
    int expectedCost = aPath.generalizedCost()
            - RaptorCostConverter.toRaptorCost((WAIT_RELUCTANCE + inverseWaitReluctance) * waitTime);

    assertEquals(expectedCost, subject.cost(aPath.accessLeg()));
  }

  @Test
  public void testAccumulationOfLogarithmicCostPart() {
    // Given:
    var aPath = BasicPathTestCase.basicTripAsPath();

    // When:
    //    - set the inverseWaitReluctance to minus waitReluctance to zero out the effect of the
    //      linear part of the function
    //    - Set a minSafeWaitTimeFactor of 1.0
    var subject = new TransferWaitTimeCalculator(WAIT_RELUCTANCE, -WAIT_RELUCTANCE, 1.0);
    // Set to 6.67% (15/100) of 120m = 1080s
    subject.setMinSafeTransferTime(1080);

    // Then: expect the cost to be equal to the original cost plus the wait-time cost
    double expectedCost = aPath.generalizedCost()
        + subject.calculateOptimizedWaitCost(WAIT_TIME_L11_L21)
        + subject.calculateOptimizedWaitCost(WAIT_TIME_L21_L31)
    ;

    assertEquals(expectedCost, subject.cost(aPath.accessLeg()), 1.0);
  }
}