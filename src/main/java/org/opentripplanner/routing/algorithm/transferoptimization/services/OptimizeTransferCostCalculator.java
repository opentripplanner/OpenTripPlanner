package org.opentripplanner.routing.algorithm.transferoptimization.services;


import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;

/**
 * This calculator uses the {@code minSafeTransferTime}(t0) and a inverse log function to calculate
 * waiting time cost. The goal is to maximize the overall waiting time and distribute the waiting
 * time evenly between each transfer.
 * <p>
 * This implementation is a BEST GUESS, we do not have any scientific support for the function
 * used.
 * <p>
 * First let look at an example. The transfer times(t) are in minutes:
 * <pre>
 *     Leg 1        Origin ---- A ----------- B
 *     Transfer 1               | t=1         | t=3
 *     Leg 2                    C ---- D ---- E ---- F
 *     Transfer 2                      | t=7         | t=4
 *     Leg 3                           G ----------- H ---- Destination(Z)
 * </pre>
 * In this example there is 3 valid paths with the following transfers:
 * <ol>
 *   <li>Alt 1: A-C and D-G with 1 + 7 = 8 minutes total</li>
 *   <li>Alt 2: A-C and F-H with 1 + 4 = 5 minutes total</li>
 *   <li>Alt 3: B-E and F-H with 3 + 4 = 7 minutes total</li>
 * </ol>
 * The best alternative here is Alt 3, with is the best compromise between maximizing the travel
 * time and avoiding the very short transfer time(1 minute) at A-C, witch is only 1 minute.
 *
 * <pre>
 *   t0 : MinSafeTransferTime
 *   N  : A constant cost factor such that f(0) = N * t0
 *
 *  N * t0  +
 *          |'
 *          |'
 *          |'
 *          | '
 *          | '
 *          | '
 *          |  '
 *          |  '
 *          |   '
 *          |    '
 *          |     '
 *          |       '
 *          |         '
 *          |            '
 *          |                '
 *          |                      '    ,
 *          |                                 '    -    ,
 *      t0  +    -   -   -   -   -   -   -   -   -   -   -   - '  -  (*)  - _ - _
 *          |
 *          :
 *
 *          |-------------|--------------|--------------|-------------|-------> time
 *          0            25%            50%            75%           t0
 *
 * The function f(t) is:
 *
 *
 *              N * minSafeTxTime
 *    f(t) = ------------------------------
 *            1 + (N - 1) * ln(1 + A * t)
 *
 *
 * The function is created so the following is true:
 *
 *   f(0) = N * t0
 *   f(t0) = t0
 *
 * A is a constant that is calculated like this:
 *
 *     A :=  (e - 1) / t0
 *
 *
 * The table below show the relative time and cost, and 2 examples where the
 * minSafeTxTime is 2 minutes and 10 minutes:
 *
 * +--------------------++----------+------------++----------+------------+
 * |       Relative     ||  minSafeTxTime is 2m  ||  minSafeTxTime is 10m |
 * +---------+----------++----------+------------++----------+------------+
 * |    t    |   cost   ||    t     |    cost    ||    t     |    cost    |
 * +---------+----------++----------+------------++----------+------------+
 * |   0.0   |   2.0    ||    0     |     240    ||    0     |    1200    |
 * |   0.1   |   1.56   ||   12s    |     188    ||    1m    |     938    |
 * |   0.2   |   1.38   ||   24s    |     166    ||    2m    |     829    |
 * |   0.5   |   1.15   ||    1m    |     138    ||    5m    |     690    |
 * |   1.0   |   1.0    ||    2m    |     120    ||   10m    |     600    |
 * |   2.0   |   0.88   ||   10m    |     105    ||   50m    |     526    |
 * |  10.0   |   0.75   ||   10m    |      90    ||   50m    |     451    |
 * +---------+----------++----------+------------++----------+------------+
 *
 * </pre>
 *
 * <b>Why use a 1/log function?</b>
 * <ol>
 *   <li>
 *     First of all we want to maximize the total transfers time across all transfers
 *     in the journey. This make sure we do not stay on a trip longer than needed and
 *     avoid back-travel.
 *   </li>
 *   <li>
 *     Second, we want to avoid VERY short transfers. The function above favor the set
 *     of transfer [3,5] over [2,6] and [2,6] over [1,7] - a linear function would not.
 *   </li>
 *   <li>
 *     Any 1/logN would be suitable, but log with base 10 is chosen (Supported in Java).
 *   </li>
 *   <li>
 *     We use this only to adjust the transfers within a single journey, not to compare
 *     two journeys with each other.
 *   </li>
 * </ol>
 */
public class OptimizeTransferCostCalculator {

  private final double n;
  private final double waitFactorCombined;
  private int t0 = -1;
  private double a = Double.NaN;


  /**
   * Set the scale factor N, so:
   * <pre>
   *   f(0) = N * minSafeTransferTime
   * </pre>
   * <p>
   *
   * @param waitReluctanceRouting The wait-reluctance used in the transit search.
   * @param inverseWaitReluctance This factor is used to calculate a cost, with should balance the
   *                              wait time against extra transit time and less walking.
   */
  public OptimizeTransferCostCalculator(
      double waitReluctanceRouting,
      double inverseWaitReluctance,
      double minSafeWaitTimeFactor
  ) {
    this.waitFactorCombined = waitReluctanceRouting + inverseWaitReluctance;
    this.n = minSafeWaitTimeFactor;
  }

  /**
   * Set the min-safe-transfer-time, Unit seconds
   */
  public void setMinSafeTransferTime(int minSafeTransferTime) {
    this.t0 = minSafeTransferTime;
    this.a = (Math.E - 1.0) / minSafeTransferTime;
  }

  public int cost(Path<?> path) {
    // We use the path generalized-cost as a starting point minus the cost of waiting.
    // We want to maximize the waiting, but balance it toward walking and time spent on-board.
    double cost = path.generalizedCost() - waitFactorCombined * path.waitTime();

    // We ignore the fact that a flex leg might have rides, because we are not considering
    // transfers between FLEX and regular transit here. That would require more knowledge about
    // the FLEX ride
    PathLeg<?> prev = path.accessLeg().nextLeg();
    PathLeg<?> curr = prev.nextLeg();

    while (!curr.isEgressLeg()) {
      // Wait time including slack
      int waitTime = curr.fromTime() - prev.toTime();

      if (curr.isTransferLeg()) {
        prev = curr;
        curr = curr.nextLeg();
        waitTime += curr.fromTime() - prev.toTime();
      }
      cost += calculateOptimizedWaitCost(waitTime);

      prev = curr;
      curr = curr.nextLeg();
    }
    return RaptorCostConverter.toRaptorCost(cost);
  }

  double calculateOptimizedWaitCost(int waitTime) {
    assertMinSafeTransferTimeSet();
    return n * t0 / (1d + (n - 1d) * Math.log1p(a * waitTime));
  }

  private void assertMinSafeTransferTimeSet() {
    if (t0 < 0) {
      throw new IllegalStateException("Min safe transfer time is not set!");
    }
  }
}
