package org.opentripplanner.updater.spi;

import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.timetable.TimetableValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a {@link TimetableValidationError} to the model of the updater ready to be consumed
 * by the metrics APIS and logs.
 */
public class DataValidationExceptionMapper {

  private static final Logger LOG = LoggerFactory.getLogger(DataValidationExceptionMapper.class);

  public static UpdateException map(DataValidationException error) {
    if (error.error() instanceof TimetableValidationError tt) {
      return UpdateException.of(tt.trip().getId(), mapTimeTableError(tt.code()), tt.stopIndex());
    }
    // The mapper should handle all possible errors
    LOG.error("Unhandled error: {}", error.getMessage(), error);
    return UpdateException.noTripId(UpdateErrorType.UNKNOWN);
  }

  private static <T> UpdateErrorType mapTimeTableError(TimetableValidationError.ErrorCode code) {
    return switch (code) {
      case NEGATIVE_DWELL_TIME -> UpdateErrorType.NEGATIVE_DWELL_TIME;
      case NEGATIVE_HOP_TIME -> UpdateErrorType.NEGATIVE_HOP_TIME;
      case MISSING_ARRIVAL_TIME -> UpdateErrorType.INVALID_ARRIVAL_TIME;
      case MISSING_DEPARTURE_TIME -> UpdateErrorType.INVALID_DEPARTURE_TIME;
    };
  }
}
