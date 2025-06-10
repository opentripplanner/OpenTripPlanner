package org.opentripplanner.routing.algorithm.mapping._support.mapping;

import org.opentripplanner.model.plan.walkstep.AbsoluteDirection;
import org.opentripplanner.routing.algorithm.mapping._support.model.ApiAbsoluteDirection;

@Deprecated
class AbsoluteDirectionMapper {

  public static ApiAbsoluteDirection mapAbsoluteDirection(AbsoluteDirection domain) {
    if (domain == null) {
      return null;
    }
    switch (domain) {
      case NORTH:
        return ApiAbsoluteDirection.NORTH;
      case NORTHEAST:
        return ApiAbsoluteDirection.NORTHEAST;
      case EAST:
        return ApiAbsoluteDirection.EAST;
      case SOUTHEAST:
        return ApiAbsoluteDirection.SOUTHEAST;
      case SOUTH:
        return ApiAbsoluteDirection.SOUTH;
      case SOUTHWEST:
        return ApiAbsoluteDirection.SOUTHWEST;
      case WEST:
        return ApiAbsoluteDirection.WEST;
      case NORTHWEST:
        return ApiAbsoluteDirection.NORTHWEST;
      default:
        throw new IllegalArgumentException(domain.toString());
    }
  }
}
