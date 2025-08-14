package org.opentripplanner.transit.model.timetable;

import org.opentripplanner.framework.error.OtpError;

/**
 * Details about why a {@link TripTimes} instance is invalid.
 */
public record TimetableValidationError(ErrorCode code, int stopIndex, Trip trip)
  implements OtpError {
  @Override
  public String errorCode() {
    return code.name();
  }

  @Override
  public String messageTemplate() {
    return "%s for stop position %d in trip %s.";
  }

  @Override
  public Object[] messageArguments() {
    return new Object[] { code, stopIndex, trip };
  }

  public enum ErrorCode {
    NEGATIVE_DWELL_TIME,
    NEGATIVE_HOP_TIME,
    MISSING_ARRIVAL_TIME,
    MISSING_DEPARTURE_TIME,
  }
}
