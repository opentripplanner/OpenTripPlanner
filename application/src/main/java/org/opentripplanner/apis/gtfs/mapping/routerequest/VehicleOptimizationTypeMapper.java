package org.opentripplanner.apis.gtfs.mapping.routerequest;

import javax.annotation.Nullable;
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

  @Nullable
  public static GraphQLTypes.GraphQLCyclingOptimizationType mapForBicycle(
    VehicleRoutingOptimizeType type
  ) {
    return switch (type) {
      case SHORTEST_DURATION -> GraphQLTypes.GraphQLCyclingOptimizationType.SHORTEST_DURATION;
      case FLAT_STREETS -> GraphQLTypes.GraphQLCyclingOptimizationType.FLAT_STREETS;
      case SAFE_STREETS -> GraphQLTypes.GraphQLCyclingOptimizationType.SAFE_STREETS;
      case SAFEST_STREETS -> GraphQLTypes.GraphQLCyclingOptimizationType.SAFEST_STREETS;
      case TRIANGLE -> null;
    };
  }

  @Nullable
  public static GraphQLTypes.GraphQLScooterOptimizationType mapForScooter(
    VehicleRoutingOptimizeType type
  ) {
    return switch (type) {
      case SHORTEST_DURATION -> GraphQLTypes.GraphQLScooterOptimizationType.SHORTEST_DURATION;
      case FLAT_STREETS -> GraphQLTypes.GraphQLScooterOptimizationType.FLAT_STREETS;
      case SAFE_STREETS -> GraphQLTypes.GraphQLScooterOptimizationType.SAFE_STREETS;
      case SAFEST_STREETS -> GraphQLTypes.GraphQLScooterOptimizationType.SAFEST_STREETS;
      case TRIANGLE -> null;
    };
  }
}
