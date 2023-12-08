package org.opentripplanner.model;

import java.util.List;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Contains a {@link TripTimes} and array of stop indices of a stop pattern that are skipped with a
 * realtime update.
 */
public class TripTimesPatch {

  private final RealTimeTripTimes tripTimes;
  private final List<Integer> skippedStopIndices;

  public TripTimesPatch(RealTimeTripTimes tripTimes, List<Integer> skippedStopIndices) {
    this.tripTimes = tripTimes;
    this.skippedStopIndices = skippedStopIndices;
  }

  public RealTimeTripTimes getTripTimes() {
    return this.tripTimes;
  }

  public List<Integer> getSkippedStopIndices() {
    return this.skippedStopIndices;
  }
}
