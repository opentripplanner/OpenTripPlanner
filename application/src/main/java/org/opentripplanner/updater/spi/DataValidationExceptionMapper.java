package org.opentripplanner.updater.spi;

import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.timetable.TimetableValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a {@link TimetableValidationError} to the model of the updater ready to be consumed
 * by the metrics APIS and logs.
 */
public class DataValidationExceptionMapper {

  private static final Logger LOG = LoggerFactory.getLogger(DataValidationExceptionMapper.class);

  public static <T> Result<T, UpdateError> toResult(DataValidationException error) {
    if (error.error() instanceof TimetableValidationError tt) {
      return Result.failure(
        new UpdateError(tt.trip().getId(), mapTimeTableError(tt.code()), tt.stopIndex())
      );
    }
    // The mapper should handle all possible errors
    LOG.error("Unhandled error: " + error.getMessage(), error);
    return Result.failure(UpdateError.noTripId(UpdateError.UpdateErrorType.UNKNOWN));
  }

  private static <T> UpdateError.UpdateErrorType mapTimeTableError(
    TimetableValidationError.ErrorCode code
  ) {
    return switch (code) {
      case NEGATIVE_DWELL_TIME -> UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME;
      case NEGATIVE_HOP_TIME -> UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME;
    };
  }
}
