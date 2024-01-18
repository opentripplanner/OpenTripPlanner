package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.routing.core.BicycleOptimizeType;

/**
 * Bicycle optimization types that are only meant to be used by the REST API. Related to {@link org.opentripplanner.routing.core.BicycleOptimizeType}
 */
public enum LegacyBicycleOptimizeType {
  QUICK,
  SAFE,
  FLAT,
  GREENWAYS,
  TRIANGLE;

  public static BicycleOptimizeType map(LegacyBicycleOptimizeType type) {
    return switch (type) {
      case QUICK -> BicycleOptimizeType.SHORTEST_DURATION;
      case FLAT -> BicycleOptimizeType.FLAT_STREETS;
      case SAFE -> BicycleOptimizeType.SAFE_STREETS;
      case GREENWAYS -> BicycleOptimizeType.SAFEST_STREETS;
      case TRIANGLE -> BicycleOptimizeType.TRIANGLE;
    };
  }
}
