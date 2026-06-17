package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.transit.model.timetable.StopRealTimeState;

/** Maps from the domain {@link StopRealTimeState} to the GTFS API {@link GraphQLTypes.GraphQLStopRealTimeState}. */
public class StopRealTimeStateMapper {

  public static GraphQLTypes.GraphQLStopRealTimeState map(StopRealTimeState state) {
    return switch (state) {
      case DEFAULT -> GraphQLTypes.GraphQLStopRealTimeState.DEFAULT;
      case INACCURATE_PREDICTIONS -> GraphQLTypes.GraphQLStopRealTimeState.INACCURATE_PREDICTIONS;
      case NO_DATA -> GraphQLTypes.GraphQLStopRealTimeState.NO_DATA;
      case RECORDED -> GraphQLTypes.GraphQLStopRealTimeState.RECORDED;
      case CANCELLED -> GraphQLTypes.GraphQLStopRealTimeState.CANCELLED;
    };
  }
}
