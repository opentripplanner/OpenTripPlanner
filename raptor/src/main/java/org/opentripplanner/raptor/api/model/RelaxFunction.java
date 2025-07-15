package org.opentripplanner.raptor.api.model;

/**
 * The relax-function is used to relax a value by in a pareto comparison. Normally this means
 * making the value bigger. We relax the right side of the pareto comparison, so the left side is
 * dominating the right side if the left value dominate the relaxed right value. For
 * arrival-time and cost we increase the value.
 */
@FunctionalInterface
public interface RelaxFunction {
  /**
   * This relax-function should give the same result as not using
   */
  RelaxFunction NORMAL = new RelaxFunction() {
    @Override
    public int relax(int value) {
      return value;
    }

    @Override
    public String toString() {
      return "NORMAL";
    }
  };

  int relax(int value);

  default boolean isNormal() {
    return this == NORMAL;
  }
}
