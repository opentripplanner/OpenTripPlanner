package org.opentripplanner.apis.gtfs.mapping;

import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.transit.model.timetable.RealTimeState;

/**
 * Maps from the internal model to the API one.
 */
public class RealtimeStateMapper {

  public static GraphQLTypes.GraphQLRealtimeState map(@Nullable RealTimeState state) {
    if (state == null) return null;
    return switch (state) {
      case SCHEDULED -> GraphQLTypes.GraphQLRealtimeState.SCHEDULED;
      case UPDATED -> GraphQLTypes.GraphQLRealtimeState.UPDATED;
      case CANCELED, DELETED -> GraphQLTypes.GraphQLRealtimeState.CANCELED;
      case ADDED -> GraphQLTypes.GraphQLRealtimeState.ADDED;
      case MODIFIED -> GraphQLTypes.GraphQLRealtimeState.MODIFIED;
    };
  }
}
