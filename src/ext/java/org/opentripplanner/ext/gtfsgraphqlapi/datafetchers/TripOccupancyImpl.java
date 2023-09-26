package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import static org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLOccupancyStatus.CRUSHED_STANDING_ROOM_ONLY;
import static org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLOccupancyStatus.EMPTY;
import static org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLOccupancyStatus.FEW_SEATS_AVAILABLE;
import static org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLOccupancyStatus.FULL;
import static org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLOccupancyStatus.MANY_SEATS_AVAILABLE;
import static org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLOccupancyStatus.NOT_ACCEPTING_PASSENGERS;
import static org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLOccupancyStatus.NO_DATA_AVAILABLE;
import static org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLOccupancyStatus.STANDING_ROOM_ONLY;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLDataFetchers;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes;
import org.opentripplanner.ext.gtfsgraphqlapi.model.TripOccupancy;

public class TripOccupancyImpl implements GraphQLDataFetchers.GraphQLTripOccupancy {

  @Override
  public DataFetcher<GraphQLTypes.GraphQLOccupancyStatus> occupancyStatus() {
    return env -> {
      var occupancyStatus = getSource(env).occupancyStatus();
      return switch (occupancyStatus) {
        case NO_DATA_AVAILABLE -> NO_DATA_AVAILABLE;
        case EMPTY -> EMPTY;
        case MANY_SEATS_AVAILABLE -> MANY_SEATS_AVAILABLE;
        case FEW_SEATS_AVAILABLE -> FEW_SEATS_AVAILABLE;
        case STANDING_ROOM_ONLY -> STANDING_ROOM_ONLY;
        case CRUSHED_STANDING_ROOM_ONLY -> CRUSHED_STANDING_ROOM_ONLY;
        case FULL -> FULL;
        case NOT_ACCEPTING_PASSENGERS -> NOT_ACCEPTING_PASSENGERS;
      };
    };
  }

  private TripOccupancy getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
