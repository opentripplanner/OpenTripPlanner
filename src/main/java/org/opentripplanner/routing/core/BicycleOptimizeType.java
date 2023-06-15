package org.opentripplanner.routing.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * When planning a bicycle route what should be optimized for. Optimize types are basically
 * combined presets of routing parameters, except for triangle.
 */
public enum BicycleOptimizeType {
  QUICK,/* the fastest trip */
  SAFE,
  FLAT,/* needs a rewrite */
  GREENWAYS,
  TRIANGLE;

  private static final Set<BicycleOptimizeType> NON_TRIANGLE_VALUES = Collections.unmodifiableSet(
    EnumSet.complementOf(EnumSet.of(TRIANGLE))
  );

  /**
   * Return all values that are not {@link BicycleOptimizeType#TRIANGLE}.
   */
  public static Set<BicycleOptimizeType> nonTriangleValues() {
    return NON_TRIANGLE_VALUES;
  }
}
