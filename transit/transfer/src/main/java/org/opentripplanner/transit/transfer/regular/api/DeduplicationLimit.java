package org.opentripplanner.transit.transfer.regular.api;

import java.util.Objects;
import org.opentripplanner.core.model.basic.Cost;

/**
 * How much more a path of a parent profile may cost than the path a profile found itself, to be
 * considered equivalent. If it is, the profile uses the path of the parent instead of storing a
 * path of its own. This saves memory, because the same path is stored once.
 * <p>
 * The tolerance is a linear function of the cost: {@code constant + coefficient * x}, where
 * {@code x} is the cost of the path of the parent, re-costed with the preferences of the profile.
 * The limit applies to cost only, not duration, since transfer discovery optimizes on cost alone.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 *
 * @param constant    the constant part of the tolerance
 * @param coefficient the part of the tolerance that grows with the cost, a share of the cost
 */
public record DeduplicationLimit(Cost constant, double coefficient) {
  /** A path within 30 seconds, and 0.5% of its cost, of the best path is equivalent. */
  public static final DeduplicationLimit DEFAULT = new DeduplicationLimit(
    Cost.costOfSeconds(30),
    0.005
  );

  public DeduplicationLimit {
    Objects.requireNonNull(constant);
    if (constant.lessThan(Cost.ZERO)) {
      throw new IllegalArgumentException("The constant must not be negative: " + constant);
    }
    if (coefficient < 0.0 || Double.isNaN(coefficient)) {
      throw new IllegalArgumentException("The coefficient must not be negative: " + coefficient);
    }
  }

  /**
   * The tolerance for a path of the parent that costs {@code c1}, in Raptor cost units
   * (centi-seconds).
   */
  public int toleranceC1(int c1) {
    return constant.toCentiSeconds() + (int) Math.round(c1 * coefficient);
  }

  /**
   * Is the path of the parent, which costs {@code parentPathC1} for the profile, equivalent to the
   * best path of the profile, which costs {@code ownC1}? Both in Raptor cost units.
   */
  public boolean isEquivalent(int parentPathC1, int ownC1) {
    return Math.abs(parentPathC1 - ownC1) <= toleranceC1(parentPathC1);
  }
}
