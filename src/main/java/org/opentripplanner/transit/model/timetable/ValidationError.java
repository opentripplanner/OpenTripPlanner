package org.opentripplanner.transit.model.timetable;

/**
 * Details about why a {@link TripTimes} instance is invalid.
 */
public record ValidationError(ErrorCode code, int stopIndex) {
  public enum ErrorCode {
    NEGATIVE_DWELL_TIME,
    NEGATIVE_HOP_TIME,
  }
}
