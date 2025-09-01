package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLVerticalTransportationType;
import org.opentripplanner.model.plan.walkstep.VerticalTransportationType;

public final class VerticalTransportationTypeMapper {

  public static GraphQLVerticalTransportationType map(VerticalTransportationType type) {
    return switch (type) {
      case ELEVATOR -> GraphQLVerticalTransportationType.ELEVATOR;
      case ESCALATOR -> GraphQLVerticalTransportationType.ESCALATOR;
      case STAIRS -> GraphQLVerticalTransportationType.STAIRS;
    };
  }
}
