package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.BicycleOptimizeType;

/**
 * Maps bicycle optimization type from API to internal model.
 */
public class BicycleOptimizationTypeMapper {

  public static BicycleOptimizeType map(GraphQLTypes.GraphQLCyclingOptimizationType type) {
    return switch (type) {
      case SHORTEST_DURATION -> BicycleOptimizeType.SHORTEST_DURATION;
      case FLAT_STREETS -> BicycleOptimizeType.FLAT_STREETS;
      case SAFE_STREETS -> BicycleOptimizeType.SAFE_STREETS;
      case SAFEST_STREETS -> BicycleOptimizeType.SAFEST_STREETS;
    };
  }
}
