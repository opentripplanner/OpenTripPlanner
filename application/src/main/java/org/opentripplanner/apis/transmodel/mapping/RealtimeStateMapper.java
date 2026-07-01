package org.opentripplanner.apis.transmodel.mapping;

import org.opentripplanner.apis.transmodel.model.TransmodelRealTimeState;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps from the internal model to the Transmodel API. See
 * {@link org.opentripplanner.apis.gtfs.mapping.RealtimeStateMapper} for GTFS (same implementation)
 */
public class RealtimeStateMapper {

  private static final Logger LOG = LoggerFactory.getLogger(RealtimeStateMapper.class);

  public static TransmodelRealTimeState map(TripTimes tripTimes) {
    return switch (tripTimes) {
      case RealTimeTripTimes realTimeTripTimes -> map(realTimeTripTimes);
      case ScheduledTripTimes _ -> TransmodelRealTimeState.SCHEDULED;
    };
  }

  private static TransmodelRealTimeState map(RealTimeTripTimes realTimeTripTimes) {
    boolean canceled = realTimeTripTimes.isCanceled();
    boolean added = realTimeTripTimes.isAdded();
    boolean modified = realTimeTripTimes.isTripPatternModified();
    boolean deleted = realTimeTripTimes.isDeleted();
    boolean updated = realTimeTripTimes.hasAnyUpdates();

    if (canceled) {
      return TransmodelRealTimeState.CANCELED;
    }
    if (deleted) {
      LOG.warn("deleted Trip {} should not be exposed to API", realTimeTripTimes.getTrip().getId());
      return TransmodelRealTimeState.CANCELED;
    }
    if (added) {
      return TransmodelRealTimeState.ADDED;
    }
    if (modified) {
      return TransmodelRealTimeState.MODIFIED;
    }
    if (updated) {
      return TransmodelRealTimeState.UPDATED;
    }
    return TransmodelRealTimeState.SCHEDULED;
  }
}
