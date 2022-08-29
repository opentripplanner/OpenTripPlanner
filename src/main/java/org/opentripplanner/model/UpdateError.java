package org.opentripplanner.model;

import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record UpdateError(@Nullable FeedScopedId tripId, UpdateErrorType errorType) {
  public enum UpdateErrorType {
    UNKNOWN,
    TRIP_ID_NOT_FOUND,
    TRIP_NOT_FOUND_IN_PATTERN,
    TRIP_ALREADY_EXISTS,
    NO_START_DATE,
    NO_UPDATES,
    TOO_FEW_STOPS,
    NO_VALID_STOPS,
    NO_SERVICE_ON_DATE,
    INVALID_ARRIVAL_TIME,
    INVALID_DEPARTURE_TIME,
    NON_INCREASING_TRIP_TIMES,
  }

  public static Optional<UpdateError> of(FeedScopedId tripId, UpdateErrorType errorType) {
    return Optional.of(new UpdateError(tripId, errorType));
  }

  public static Optional<UpdateError> noError() {
    return Optional.empty();
  }

  public static UpdateError noTripId(UpdateErrorType errorType) {
    return new UpdateError(null, errorType);
  }
}
