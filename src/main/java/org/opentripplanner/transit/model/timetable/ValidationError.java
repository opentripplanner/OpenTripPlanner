package org.opentripplanner.transit.model.timetable;

/**
 * Details about why a {@link TripTimes} instance is invalid.
 */
public record ValidationError(ValidationErrorType type, int stopIndex) {
  public enum ValidationErrorType {
    NEGATIVE_DWELL_TIME,
    NEGATIVE_HOP_TIME,
  }
}
