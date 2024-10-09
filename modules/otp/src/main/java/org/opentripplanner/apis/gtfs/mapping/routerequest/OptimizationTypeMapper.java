package org.opentripplanner.apis.gtfs.mapping.routerequest;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

public final class OptimizationTypeMapper {

  public static VehicleRoutingOptimizeType map(GraphQLTypes.GraphQLOptimizeType optimizeType) {
    return switch (optimizeType) {
      case QUICK -> VehicleRoutingOptimizeType.SHORTEST_DURATION;
      case FLAT -> VehicleRoutingOptimizeType.FLAT_STREETS;
      case SAFE -> VehicleRoutingOptimizeType.SAFE_STREETS;
      case GREENWAYS -> VehicleRoutingOptimizeType.SAFEST_STREETS;
      case TRIANGLE -> VehicleRoutingOptimizeType.TRIANGLE;
    };
  }
}
