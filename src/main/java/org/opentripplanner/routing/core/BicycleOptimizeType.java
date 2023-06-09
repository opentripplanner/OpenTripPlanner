package org.opentripplanner.routing.core;

import java.util.Arrays;
import java.util.stream.Stream;

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

  /**
   * Return all values that are not {@link BicycleOptimizeType#TRIANGLE}.
   */
  public static Stream<BicycleOptimizeType> nonTriangleValues() {
    return Arrays.stream(values()).filter(t -> t != BicycleOptimizeType.TRIANGLE);
  }
}
