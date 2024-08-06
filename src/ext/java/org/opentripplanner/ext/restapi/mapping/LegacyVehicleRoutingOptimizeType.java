package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

/**
 * Bicycle and scooter optimization types that are only meant to be used by the REST API. Related to
 * {@link VehicleRoutingOptimizeType}
 */
public enum LegacyVehicleRoutingOptimizeType {
  QUICK,
  SAFE,
  FLAT,
  GREENWAYS,
  TRIANGLE;

  public static VehicleRoutingOptimizeType map(LegacyVehicleRoutingOptimizeType type) {
    return switch (type) {
      case QUICK -> VehicleRoutingOptimizeType.SHORTEST_DURATION;
      case FLAT -> VehicleRoutingOptimizeType.FLAT_STREETS;
      case SAFE -> VehicleRoutingOptimizeType.SAFE_STREETS;
      case GREENWAYS -> VehicleRoutingOptimizeType.SAFEST_STREETS;
      case TRIANGLE -> VehicleRoutingOptimizeType.TRIANGLE;
    };
  }
}
