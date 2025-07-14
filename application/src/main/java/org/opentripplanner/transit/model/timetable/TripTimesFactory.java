package org.opentripplanner.transit.model.timetable;

import java.util.List;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.framework.Deduplicator;

/**
 * The responsibility of this class is to create TripTimes based on StopTimes. The
 * TripTimes should not have a dependency to StopTimes, so this class act as a middleman.
 * Eventually this class should not be needed - the intermediate step to map feeds into
 * StopTimes and then map stop-times into TripTimes is unnecessary - we should map the
 * feeds directly into {@link ScheduledTripTimes} using the builder instead.
 */
public class TripTimesFactory {

  /**
   * The provided stopTimes are assumed to be pre-filtered, valid, and monotonically increasing. The
   * non-interpolated stoptimes should already be marked at timepoints by a previous filtering
   * step.
   */
  public static ScheduledTripTimes tripTimes(
    Trip trip,
    List<StopTime> stopTimes,
    Deduplicator deduplicator
  ) {
    return StopTimeToScheduledTripTimesMapper.map(trip, stopTimes, deduplicator);
  }
}
