package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLVerticalDirection;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.VerticalDirection;

public class VerticalDirectionMapper {

  public static GraphQLVerticalDirection map(VerticalDirection verticalDirection) {
    return switch (verticalDirection) {
      case DOWN -> GraphQLVerticalDirection.DOWN;
      case UP -> GraphQLVerticalDirection.UP;
      case UNKNOWN -> GraphQLVerticalDirection.UNKNOWN;
    };
  }
}
