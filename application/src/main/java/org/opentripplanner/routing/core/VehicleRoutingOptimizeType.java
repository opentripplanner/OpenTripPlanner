package org.opentripplanner.routing.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * When planning a bicycle or scooter route what should be optimized for. Optimize types are
 * basically combined presets of routing parameters, except for triangle.
 */
public enum VehicleRoutingOptimizeType {
  /** This was previously called QUICK */
  SHORTEST_DURATION,
  /** This was previously called SAFE */
  SAFE_STREETS,
  /** This was previously called FLAT. Needs a rewrite. */
  FLAT_STREETS,
  /** This was previously called GREENWAYS. */
  SAFEST_STREETS,
  TRIANGLE;

  private static final Set<VehicleRoutingOptimizeType> NON_TRIANGLE_VALUES =
    Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(TRIANGLE)));

  /**
   * Return all values that are not {@link VehicleRoutingOptimizeType#TRIANGLE}.
   */
  public static Set<VehicleRoutingOptimizeType> nonTriangleValues() {
    return NON_TRIANGLE_VALUES;
  }
}
