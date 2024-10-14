package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.model.TripOccupancy;

public class TripOccupancyImpl implements GraphQLDataFetchers.GraphQLTripOccupancy {

  @Override
  public DataFetcher<GraphQLTypes.GraphQLOccupancyStatus> occupancyStatus() {
    return env -> {
      var occupancyStatus = getSource(env).occupancyStatus();
      return switch (occupancyStatus) {
        case NO_DATA_AVAILABLE -> GraphQLTypes.GraphQLOccupancyStatus.NO_DATA_AVAILABLE;
        case EMPTY -> GraphQLTypes.GraphQLOccupancyStatus.EMPTY;
        case MANY_SEATS_AVAILABLE -> GraphQLTypes.GraphQLOccupancyStatus.MANY_SEATS_AVAILABLE;
        case FEW_SEATS_AVAILABLE -> GraphQLTypes.GraphQLOccupancyStatus.FEW_SEATS_AVAILABLE;
        case STANDING_ROOM_ONLY -> GraphQLTypes.GraphQLOccupancyStatus.STANDING_ROOM_ONLY;
        case CRUSHED_STANDING_ROOM_ONLY -> GraphQLTypes.GraphQLOccupancyStatus.CRUSHED_STANDING_ROOM_ONLY;
        case FULL -> GraphQLTypes.GraphQLOccupancyStatus.FULL;
        case NOT_ACCEPTING_PASSENGERS -> GraphQLTypes.GraphQLOccupancyStatus.NOT_ACCEPTING_PASSENGERS;
      };
    };
  }

  private TripOccupancy getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
