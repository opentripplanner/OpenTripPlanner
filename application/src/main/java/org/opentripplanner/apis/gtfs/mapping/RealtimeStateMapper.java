package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps from the internal model to the GTFS API. See
 * {@link org.opentripplanner.apis.transmodel.mapping.RealtimeStateMapper} for transmodel (same
 * implementation)
 */
public class RealtimeStateMapper {

  private static final Logger LOG = LoggerFactory.getLogger(RealtimeStateMapper.class);

  public static GraphQLTypes.GraphQLRealtimeState map(TripTimes tripTimes) {
    return switch (tripTimes) {
      case RealTimeTripTimes realTimeTripTimes -> map(realTimeTripTimes);
      case ScheduledTripTimes _ -> GraphQLTypes.GraphQLRealtimeState.SCHEDULED;
    };
  }

  private static GraphQLTypes.GraphQLRealtimeState map(RealTimeTripTimes realTimeTripTimes) {
    boolean canceled = realTimeTripTimes.isCanceled();
    boolean added = realTimeTripTimes.isAdded();
    boolean modified = realTimeTripTimes.isTripPatternModified();
    boolean deleted = realTimeTripTimes.isDeleted();
    boolean updated = realTimeTripTimes.hasAnyUpdates();

    if (canceled) {
      return GraphQLTypes.GraphQLRealtimeState.CANCELED;
    }
    if (deleted) {
      LOG.warn("deleted Trip {} should not be exposed to API", realTimeTripTimes.getTrip().getId());
      return GraphQLTypes.GraphQLRealtimeState.CANCELED;
    }
    if (added) {
      return GraphQLTypes.GraphQLRealtimeState.ADDED;
    }
    if (modified) {
      return GraphQLTypes.GraphQLRealtimeState.MODIFIED;
    }
    if (updated) {
      return GraphQLTypes.GraphQLRealtimeState.UPDATED;
    }
    return GraphQLTypes.GraphQLRealtimeState.SCHEDULED;
  }
}
