package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopPositionType;
import org.opentripplanner.model.StopPositionType;

public class TripPositionMapper {

  public static GraphQLStopPositionType map(StopPositionType tripPosition) {
    return switch (tripPosition) {
      case FIRST -> GraphQLStopPositionType.FIRST;
      case MIDDLE -> GraphQLStopPositionType.MIDDLE;
      case LAST -> GraphQLStopPositionType.LAST;
    };
  }
}
