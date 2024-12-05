package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import org.opentripplanner.raptor.api.model.RaptorCostConverter;

/**
 * This {@link FactorStrategy} keep a single value and use it every time the factor is needed. The
 * {@link #minFactor()} return the same value.
 * <p>
 * The class and methods are {@code final} to help the JIT compiler optimize the use of this class.
 */
final class SingleValueFactorStrategy implements FactorStrategy {

  private final int factor;

  SingleValueFactorStrategy(int factor) {
    this.factor = factor;
  }

  SingleValueFactorStrategy(double reluctance) {
    this(RaptorCostConverter.toRaptorCost(reluctance));
  }

  @Override
  public int factor(int index) {
    return factor;
  }

  @Override
  public int minFactor() {
    return factor;
  }
}
