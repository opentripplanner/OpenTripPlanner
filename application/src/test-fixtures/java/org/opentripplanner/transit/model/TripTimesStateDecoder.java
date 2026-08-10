package org.opentripplanner.transit.model;

import org.opentripplanner.transit.model.timetable.TripTimes;

public class TripTimesStateDecoder {

  /// Decodes the state booleans a trip can have into a human-readable String representation:
  /// * A -> Added Trip
  /// * C -> Canceled trip
  /// * P -> Trip Pattern was modified
  /// * D -> Deleted Trip
  /// * U -> Trip received any sort of update (times, cancellation etc.)
  /// * S -> Trip received no updates
  public static String summarizeFromTripTimes(TripTimes tripTimes) {
    StringBuilder stringBuilder = new StringBuilder();
    if (tripTimes.isAdded()) {
      stringBuilder.append("A ");
    }
    if (tripTimes.isCanceled()) {
      stringBuilder.append("C ");
    }
    if (tripTimes.isTripPatternModified()) {
      stringBuilder.append("P ");
    }
    if (tripTimes.isDeleted()) {
      stringBuilder.append("D ");
    }
    if (tripTimes.hasAnyUpdates()) {
      stringBuilder.append("U ");
    } else {
      stringBuilder.append("S ");
    }
    return stringBuilder.toString().trim();
  }
}
