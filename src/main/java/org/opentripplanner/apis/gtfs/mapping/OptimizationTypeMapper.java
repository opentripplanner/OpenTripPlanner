package org.opentripplanner.apis.gtfs.mapping;

import javax.annotation.Nonnull;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.core.BicycleOptimizeType;

public final class OptimizationTypeMapper {

  @Nonnull
  public static BicycleOptimizeType map(GraphQLTypes.GraphQLOptimizeType optimizeType) {
    return switch (optimizeType) {
      case QUICK -> BicycleOptimizeType.SHORTEST_DURATION;
      case FLAT -> BicycleOptimizeType.FLAT_STREETS;
      case SAFE -> BicycleOptimizeType.SAFE_STREETS;
      case GREENWAYS -> BicycleOptimizeType.SAFEST_STREETS;
      case TRIANGLE -> BicycleOptimizeType.TRIANGLE;
    };
  }
}
