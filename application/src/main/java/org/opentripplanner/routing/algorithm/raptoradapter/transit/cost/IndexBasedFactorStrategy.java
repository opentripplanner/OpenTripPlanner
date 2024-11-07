package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.Arrays;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;

/**
 * This class keep a facto for each index and the minimum factor for fast retrieval during Raptor
 * search.
 */
final class IndexBasedFactorStrategy implements FactorStrategy {

  private final int[] factors;
  private final int minFactor;

  private IndexBasedFactorStrategy(int[] factors) {
    this.factors = factors;
    this.minFactor = findMinimumFactor(factors);
  }

  /** Convert OTP domain reluctance array to Raptor factors. */
  IndexBasedFactorStrategy(double[] reluctanceByIndex) {
    this(RaptorCostConverter.toRaptorCosts(reluctanceByIndex));
  }

  @Override
  public int factor(int index) {
    return factors[index];
  }

  @Override
  public int minFactor() {
    return minFactor;
  }

  private static int findMinimumFactor(int[] factors) {
    return Arrays.stream(factors).min().orElseThrow();
  }
}
