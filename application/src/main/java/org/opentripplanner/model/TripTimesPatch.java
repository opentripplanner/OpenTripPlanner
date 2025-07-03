package org.opentripplanner.model;

import java.util.Map;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Contains a {@link TripTimes} and array of stop indices of a stop pattern that are skipped with a
 * realtime update.
 */
public record TripTimesPatch(
  RealTimeTripTimes tripTimes,
  Map<Integer, PickDrop> updatedPickup,
  Map<Integer, PickDrop> updatedDropoff,
  Map<Integer, String> replacedStopIndices
) {}
