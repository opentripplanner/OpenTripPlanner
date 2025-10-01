package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLInclineType;
import org.opentripplanner.model.plan.walkstep.verticaltransportationuse.InclineType;

public class InclineTypeMapper {

  public static GraphQLInclineType map(InclineType type) {
    return switch (type) {
      case DOWN -> GraphQLInclineType.DOWN;
      case UP -> GraphQLInclineType.UP;
    };
  }
}
