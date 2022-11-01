package org.opentripplanner.model;

import java.util.List;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Contains a {@link TripTimes} and array of stop indices of a stop pattern that are skipped with a
 * realtime update.
 */
public class TripTimesPatch {

  private final TripTimes tripTimes;
  private final List<Integer> skippedStopIndices;

  public TripTimesPatch(TripTimes tripTimes, List<Integer> skippedStopIndices) {
    this.tripTimes = tripTimes;
    this.skippedStopIndices = skippedStopIndices;
  }

  public TripTimes getTripTimes() {
    return this.tripTimes;
  }

  public List<Integer> getSkippedStopIndices() {
    return this.skippedStopIndices;
  }
}
