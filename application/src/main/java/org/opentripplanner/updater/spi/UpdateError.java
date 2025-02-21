package org.opentripplanner.updater.spi;

import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;

/**
 * Detailed information about a failure to apply a realtime update, for example for trips or vehicle
 * positions.
 */
public record UpdateError(
  @Nullable FeedScopedId tripId,
  UpdateErrorType errorType,
  @Nullable Integer stopIndex,
  @Nullable String producer
) {
  public UpdateError(@Nullable FeedScopedId tripId, UpdateErrorType errorType) {
    this(tripId, errorType, null, null);
  }

  public UpdateError(@Nullable FeedScopedId tripId, UpdateErrorType errorType, Integer stopIndex) {
    this(tripId, errorType, stopIndex, null);
  }
  public UpdateError(@Nullable FeedScopedId tripId, UpdateErrorType errorType, String producer) {
    this(tripId, errorType, null, producer);
  }

  public String debugId() {
    if (tripId == null) {
      return "no trip id";
    } else if (stopIndex == null) {
      return tripId.toString();
    } else {
      return "%s{stopIndex=%s}".formatted(tripId, stopIndex);
    }
  }

  public enum UpdateErrorType {
    UNKNOWN,
    INVALID_INPUT_STRUCTURE,
    TRIP_NOT_FOUND,
    TRIP_NOT_FOUND_IN_PATTERN,
    NO_FUZZY_TRIP_MATCH,
    MULTIPLE_FUZZY_TRIP_MATCHES,
    EMPTY_STOP_POINT_REF,
    NO_TRIP_FOR_CANCELLATION_FOUND,
    TRIP_ALREADY_EXISTS,
    NO_START_DATE,
    NO_UPDATES,
    NO_TRIP_ID,
    TOO_FEW_STOPS,
    NO_VALID_STOPS,
    NO_SERVICE_ON_DATE,
    INVALID_ARRIVAL_TIME,
    INVALID_DEPARTURE_TIME,
    NEGATIVE_DWELL_TIME,
    NEGATIVE_HOP_TIME,
    INVALID_STOP_SEQUENCE,
    NOT_IMPLEMENTED_UNSCHEDULED,
    NOT_IMPLEMENTED_DUPLICATED,
    NOT_MONITORED,
    CANNOT_RESOLVE_AGENCY,
  }

  public static <T> Result<T, UpdateError> result(FeedScopedId tripId, UpdateErrorType errorType) {
    return Result.failure(new UpdateError(tripId, errorType));
  }

  public static <T> Result<T, UpdateError> result(
    FeedScopedId tripId,
    UpdateErrorType errorType,
    String producer
  ) {
    return Result.failure(new UpdateError(tripId, errorType, producer));
  }

  public static UpdateError noTripId(UpdateErrorType errorType) {
    return new UpdateError(null, errorType);
  }
}
