package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.WAIT_TIME_L11_L21;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.WAIT_TIME_L21_L31;
import static org.opentripplanner.util.time.DurationUtils.duration;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase;

public class OptimizeTransferCostCalculatorTest {
  private static final double WAIT_RELUCTANCE = 0.5;
  private static final double EPSILON = 0.01;

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

  private OptimizeTransferCostCalculator subject;


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
      subject = new OptimizeTransferCostCalculator(WAIT_RELUCTANCE,0.5, n);

      for (int t0 : ts) {
        subject.setMinSafeTransferTime(t0);
        String testCase = String.format("t0=%d, n=%.1f", t0, n);

        assertEquals("f(0) with " + testCase, n * t0, subject.calculateOptimizedWaitCost(zero), EPSILON);
        assertEquals("f(t0) with " + testCase, t0, subject.calculateOptimizedWaitCost(t0), EPSILON);
      }
    }
  }

  @Test
  public void calculateTxCost_sample_A() {
    subject = new OptimizeTransferCostCalculator(WAIT_RELUCTANCE, 1.0, 2.0);
    subject.setMinSafeTransferTime(d2m);

    assertEquals(236.64, subject.calculateOptimizedWaitCost(1), EPSILON);
    assertEquals(207.15, subject.calculateOptimizedWaitCost(d12s), EPSILON);
    assertEquals(185.27, subject.calculateOptimizedWaitCost(d24s), EPSILON);
    assertEquals(148.14, subject.calculateOptimizedWaitCost(d1m), EPSILON);
    assertEquals(96.39, subject.calculateOptimizedWaitCost(d4m), EPSILON);
    assertEquals( 73.60, subject.calculateOptimizedWaitCost(d10m), EPSILON);
    assertEquals( 24.67, subject.calculateOptimizedWaitCost(d5d), EPSILON);
  }

  @Test
  public void calculateTxCost_sample_B() {
    subject = new OptimizeTransferCostCalculator(WAIT_RELUCTANCE, 1.0, 5.0);
    subject.setMinSafeTransferTime(d10m);

    assertEquals(2966.07, subject.calculateOptimizedWaitCost(1), EPSILON);
    assertEquals(1835.69, subject.calculateOptimizedWaitCost(d1m), EPSILON);
    assertEquals(1375.15, subject.calculateOptimizedWaitCost(d2m), EPSILON);
    assertEquals(861.96, subject.calculateOptimizedWaitCost(d5m), EPSILON);
    assertEquals(431.06, subject.calculateOptimizedWaitCost(d20m), EPSILON);
    assertEquals(298.70, subject.calculateOptimizedWaitCost(d50m), EPSILON);
    assertEquals(101.74, subject.calculateOptimizedWaitCost(d5d), EPSILON);
  }

  @Test(expected = IllegalStateException.class)
  public void calculateTxCostWithNoMinSafeTxTimeThrowsException() {
    var subject = new OptimizeTransferCostCalculator(WAIT_RELUCTANCE, 1.0, 2.0);
    subject.calculateOptimizedWaitCost(d20m);
  }

  @Test
  public void testLinearCostPart() {
    // Given:
    var aPath = BasicPathTestCase.basicTripAsPath();
    double inverseWaitReluctance = 0.5;
    int waitTime = aPath.waitTime();

    // When: set min-safe-wait-time-factor to zero(0) and min-safe-transfer-time to any value
    var subject = new OptimizeTransferCostCalculator(WAIT_RELUCTANCE, inverseWaitReluctance, 0.0);
    subject.setMinSafeTransferTime(99999999);

    // Then: expect the cost to be equal to the original cost minus cost of waiting
    int expectedCost = aPath.generalizedCost() - (int)((WAIT_RELUCTANCE + inverseWaitReluctance) * waitTime);
    assertEquals(expectedCost, subject.cost(aPath)/100);
  }

  @Test
  public void testAccumulationOfLogarithmicCostPart() {
    // Given:
    var aPath = BasicPathTestCase.basicTripAsPath();

    // When:
    //    - set the inverseWaitReluctance to minus waitReluctance to zero out the effect of the
    //      linear part of the function
    //    - Set a minSafeWaitTimeFactor of 1.0
    var subject = new OptimizeTransferCostCalculator(WAIT_RELUCTANCE, -WAIT_RELUCTANCE, 1.0);
    // Set to 6.67% (15/100) of 120m = 1080s
    subject.setMinSafeTransferTime(1080);

    System.out.println(subject.calculateOptimizedWaitCost(WAIT_TIME_L11_L21));
    System.out.println(subject.calculateOptimizedWaitCost(WAIT_TIME_L21_L31));

    // Then: expect the cost to be equal to the original cost plus the wait-time cost
    double expectedCost = 100.0 * (
        aPath.generalizedCost()
        + subject.calculateOptimizedWaitCost(WAIT_TIME_L11_L21)
        + subject.calculateOptimizedWaitCost(WAIT_TIME_L21_L31)
    );

    assertEquals(expectedCost, subject.cost(aPath), 1.0);
  }
}