package org.opentripplanner.updater;

import org.opentripplanner.model.UpdateError;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Converts the result of a {@link TripTimes} to the model of the updater ready to be consumed
 * by the metrics.
 */
public class TripTimesValidationMapper {

  public static <T> Result<T, UpdateError> toResult(
    FeedScopedId tripId,
    TripTimes.ValidationError validationError
  ) {
    var type =
      switch (validationError.type()) {
        case NEGATIVE_DWELL_TIME -> UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME;
        case NEGATIVE_HOP_TIME -> UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME;
      };

    return Result.failure(new UpdateError(tripId, type, validationError.stopIndex()));
  }
}
