package org.opentripplanner.apis.gtfs.mapping.routerequest;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

/**
 * Maps vehicle optimization type from API to internal model.
 */
public class VehicleOptimizationTypeMapper {

  public static VehicleRoutingOptimizeType map(GraphQLTypes.GraphQLCyclingOptimizationType type) {
    return switch (type) {
      case SHORTEST_DURATION -> VehicleRoutingOptimizeType.SHORTEST_DURATION;
      case FLAT_STREETS -> VehicleRoutingOptimizeType.FLAT_STREETS;
      case SAFE_STREETS -> VehicleRoutingOptimizeType.SAFE_STREETS;
      case SAFEST_STREETS -> VehicleRoutingOptimizeType.SAFEST_STREETS;
    };
  }

  public static VehicleRoutingOptimizeType map(GraphQLTypes.GraphQLScooterOptimizationType type) {
    return switch (type) {
      case SHORTEST_DURATION -> VehicleRoutingOptimizeType.SHORTEST_DURATION;
      case FLAT_STREETS -> VehicleRoutingOptimizeType.FLAT_STREETS;
      case SAFE_STREETS -> VehicleRoutingOptimizeType.SAFE_STREETS;
      case SAFEST_STREETS -> VehicleRoutingOptimizeType.SAFEST_STREETS;
    };
  }
}
