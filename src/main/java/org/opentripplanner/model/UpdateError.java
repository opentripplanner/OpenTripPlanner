package org.opentripplanner.model;

import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.common.model.Result;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record UpdateError(@Nullable FeedScopedId tripId, UpdateErrorType errorType) {
  public enum UpdateErrorType {
    UNKNOWN,
    INVALID_INPUT_STRUCTURE,
    TRIP_NOT_FOUND,
    TRIP_NOT_FOUND_IN_PATTERN,
    NO_FUZZY_TRIP_MATCH,
    NO_TRIP_FOR_CANCELLATION_FOUND,
    TRIP_ALREADY_EXISTS,
    NO_START_DATE,
    NO_UPDATES,
    TOO_FEW_STOPS,
    NO_VALID_STOPS,
    NO_SERVICE_ON_DATE,
    INVALID_ARRIVAL_TIME,
    INVALID_DEPARTURE_TIME,
    NON_INCREASING_TRIP_TIMES,
    INVALID_STOP_SEQUENCE,
    NOT_IMPLEMENTED_UNSCHEDULED,
    NOT_IMPLEMENTED_DUPLICATED,
    NOT_MONITORED,
  }

  public static Optional<UpdateError> optional(FeedScopedId tripId, UpdateErrorType errorType) {
    return Optional.of(new UpdateError(tripId, errorType));
  }

  public static <T> Result<T, UpdateError> result(FeedScopedId tripId, UpdateErrorType errorType) {
    return Result.failure(new UpdateError(tripId, errorType));
  }

  public static Optional<UpdateError> noError() {
    return Optional.empty();
  }

  public static UpdateError noTripId(UpdateErrorType errorType) {
    return new UpdateError(null, errorType);
  }
}
