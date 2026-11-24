package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps from the internal model to the GTFS API. See
 * {@link org.opentripplanner.apis.transmodel.mapping.RealtimeStateMapper} for transmodel.
 */
public class RealtimeStateMapper {

  private static final Logger LOG = LoggerFactory.getLogger(RealtimeStateMapper.class);

  public static GraphQLTypes.GraphQLRealtimeState map(TripTimes<?> tripTimes) {
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

  /**
   * The real-time state of a single stop of a trip. A canceled stop, or a stop of a canceled trip,
   * is {@code CANCELED}. Otherwise it is the real-time state of its trip, except in a trip with a
   * modified stop pattern (e.g. by a GTFS-RT {@code REPLACEMENT} trip update or a SIRI extra call).
   * In such a trip, an added stop is {@code ADDED} and a stop without real-time data
   * ({@code NO_DATA}) is {@code SCHEDULED}.
   */
  public static GraphQLTypes.GraphQLRealtimeState map(TripTimeOnDate stopTime) {
    if (stopTime.isCanceledEffectively()) {
      return GraphQLTypes.GraphQLRealtimeState.CANCELED;
    }
    var tripState = map(stopTime.getTripTimes());
    if (tripState == GraphQLTypes.GraphQLRealtimeState.MODIFIED) {
      if (stopTime.isExtraCall()) {
        return GraphQLTypes.GraphQLRealtimeState.ADDED;
      }
      if (stopTime.isNoDataStop()) {
        return GraphQLTypes.GraphQLRealtimeState.SCHEDULED;
      }
    }
    return tripState;
  }
}
