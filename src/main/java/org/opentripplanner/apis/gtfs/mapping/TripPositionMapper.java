package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLTripPosition;
import org.opentripplanner.model.PositionInTrip;

public class TripPositionMapper {

  public static GraphQLTripPosition map(PositionInTrip tripPosition) {
    return switch (tripPosition) {
      case FIRST -> GraphQLTripPosition.FIRST;
      case MIDDLE -> GraphQLTripPosition.MIDDLE;
      case LAST -> GraphQLTripPosition.LAST;
    };
  }
}
