package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.utils.time.DurationUtils.durationInSeconds;

import org.junit.jupiter.api.Test;

public class TransferWaitTimeCostCalculatorTest {

  private static final double EPSILON = 0.005;
  private static final double ANY = Double.NaN;

  final int d12s = durationInSeconds("12s");
  final int d24s = durationInSeconds("24s");
  final int d1m = durationInSeconds("1m");
  final int d2m = durationInSeconds("2m");
  final int d4m = durationInSeconds("4m");
  final int d5m = durationInSeconds("5m");
  final int d10m = durationInSeconds("10m");
  final int d20m = durationInSeconds("20m");
  final int d50m = durationInSeconds("50m");
  final int d5d = 5 * durationInSeconds("24h");

  private TransferWaitTimeCostCalculator subject;

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
      subject = new TransferWaitTimeCostCalculator(0.5, n);

      for (int t0 : ts) {
        subject.setMinSafeTransferTime(t0);
        String testCase = String.format("t0=%d, n=%.1f", t0, n);

        assertEquals(
          n * t0,
          subject.avoidShortWaitTimeCost(zero),
          EPSILON,
          "f(0) with " + testCase
        );
        assertEquals(t0, subject.avoidShortWaitTimeCost(t0), EPSILON, "f(t0) with " + testCase);
      }
    }
  }

  @Test
  public void avoidShortWaitTimeCost_sample_A() {
    subject = new TransferWaitTimeCostCalculator(ANY, 2.0);
    subject.setMinSafeTransferTime(d2m);

    assertEquals(236.64, subject.avoidShortWaitTimeCost(1), EPSILON);
    assertEquals(207.15, subject.avoidShortWaitTimeCost(d12s), EPSILON);
    assertEquals(185.27, subject.avoidShortWaitTimeCost(d24s), EPSILON);
    assertEquals(148.14, subject.avoidShortWaitTimeCost(d1m), EPSILON);
    assertEquals(96.39, subject.avoidShortWaitTimeCost(d4m), EPSILON);
    assertEquals(73.60, subject.avoidShortWaitTimeCost(d10m), EPSILON);
    assertEquals(24.67, subject.avoidShortWaitTimeCost(d5d), EPSILON);
  }

  @Test
  public void avoidShortWaitTimeCost_sample_B() {
    subject = new TransferWaitTimeCostCalculator(ANY, 5.0);
    subject.setMinSafeTransferTime(d10m);

    assertEquals(2966.07, subject.avoidShortWaitTimeCost(1), EPSILON);
    assertEquals(1835.69, subject.avoidShortWaitTimeCost(d1m), EPSILON);
    assertEquals(1375.15, subject.avoidShortWaitTimeCost(d2m), EPSILON);
    assertEquals(861.96, subject.avoidShortWaitTimeCost(d5m), EPSILON);
    assertEquals(431.06, subject.avoidShortWaitTimeCost(d20m), EPSILON);
    assertEquals(298.70, subject.avoidShortWaitTimeCost(d50m), EPSILON);
    assertEquals(101.74, subject.avoidShortWaitTimeCost(d5d), EPSILON);
  }

  @Test
  public void avoidBackTravelCost() {
    subject = new TransferWaitTimeCostCalculator(0.5, ANY);
    // MinSafeTransferTime should not have an effect on the test, hence any value
    subject.setMinSafeTransferTime(Integer.MAX_VALUE);

    assertEquals(0, subject.avoidBackTravelCost(0), EPSILON);
    assertEquals(-30.0, subject.avoidBackTravelCost(d1m), EPSILON);
  }

  @Test
  public void calculateOptimizedWaitCost() {
    subject = new TransferWaitTimeCostCalculator(0.5, 5.0);
    subject.setMinSafeTransferTime(d10m);

    // Combine test avoidShortWaitTimeCost_sample_B and avoidBackTravelCost
    assertEquals(180569.0, subject.calculateOptimizedWaitCost(d1m), EPSILON);
  }

  @Test
  public void calculateTxCostWithNoMinSafeTxTimeThrowsException() {
    assertThrows(IllegalStateException.class, () -> {
      var subject = new TransferWaitTimeCostCalculator(1.0, 2.0);
      subject.calculateOptimizedWaitCost(d20m);
    });
  }

  @Test
  void calculateStaySeatedTransferCost() {
    subject = new TransferWaitTimeCostCalculator(1d, 1d);
    assertEquals(-10_000_000, subject.calculateStaySeatedTransferCost());
  }

  @Test
  void calculateGuaranteedTransferCost() {
    subject = new TransferWaitTimeCostCalculator(1d, 1d);
    assertEquals(-5_000_000, subject.calculateGuaranteedTransferCost());
  }
}
