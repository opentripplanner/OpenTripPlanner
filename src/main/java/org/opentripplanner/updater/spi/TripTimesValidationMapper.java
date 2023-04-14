package org.opentripplanner.updater.spi;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.timetable.ValidationError;

/**
 * Converts a {@link ValidationError} to the model of the updater ready to be consumed
 * by the metrics APIS and logs.
 */
public class TripTimesValidationMapper {

  public static <T> Result<T, UpdateError> toResult(
    FeedScopedId tripId,
    ValidationError validationError
  ) {
    var type =
      switch (validationError.type()) {
        case NEGATIVE_DWELL_TIME -> UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME;
        case NEGATIVE_HOP_TIME -> UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME;
      };

    return Result.failure(new UpdateError(tripId, type, validationError.stopIndex()));
  }
}
