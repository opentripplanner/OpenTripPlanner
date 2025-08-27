package org.opentripplanner.updater.trip.gtfs.model;

import java.util.Map;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Contains a {@link TripTimes} and array of stop indices of a stop pattern that are skipped with a
 * realtime update.
 */
public final class TripTimesPatch {

  private final RealTimeTripTimes tripTimes;
  private final Map<Integer, PickDrop> updatedPickup;
  private final Map<Integer, PickDrop> updatedDropoff;
  private final Map<Integer, String> replacedStopIndices;

  public TripTimesPatch(
    RealTimeTripTimes tripTimes,
    Map<Integer, PickDrop> updatedPickup,
    Map<Integer, PickDrop> updatedDropoff,
    Map<Integer, String> replacedStopIndices
  ) {
    this.tripTimes = tripTimes;
    this.updatedPickup = updatedPickup;
    this.updatedDropoff = updatedDropoff;
    this.replacedStopIndices = replacedStopIndices;
  }

  public RealTimeTripTimes tripTimes() {
    return tripTimes;
  }

  public Map<Integer, PickDrop> updatedPickup() {
    return updatedPickup;
  }

  public Map<Integer, PickDrop> updatedDropoff() {
    return updatedDropoff;
  }

  public Map<Integer, String> replacedStopIndices() {
    return replacedStopIndices;
  }
}
