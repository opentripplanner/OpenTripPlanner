package org.opentripplanner.updater.spi;

import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;

public class UpdateException extends RuntimeException {

  private final UpdateErrorType errorType;

  @Nullable
  private final FeedScopedId tripId;

  private final @Nullable Integer stopIndex;
  private final @Nullable String producer;

  public UpdateException(
    @Nullable FeedScopedId tripId,
    UpdateErrorType errorType,
    @Nullable Integer stopIndex,
    @Nullable String producer
  ) {
    this.tripId = tripId;
    this.errorType = errorType;
    this.stopIndex = stopIndex;
    this.producer = producer;
  }

  public static UpdateException of(FeedScopedId tripId, UpdateErrorType errorType) {
    return new UpdateException(tripId, errorType, null, null);
  }

  public static UpdateException of(
    FeedScopedId tripId,
    UpdateErrorType errorType,
    String producer
  ) {
    return new UpdateException(tripId, errorType, null, producer);
  }

  public static UpdateException ofStopIndex(UpdateErrorType updateErrorType, int stopIndex) {
    return new UpdateException(null, updateErrorType, stopIndex, null);
  }

  public static UpdateException noTripId(UpdateErrorType errorType) {
    return new UpdateException(null, errorType, null, null);
  }

  public static UpdateException of(UpdateErrorType updateErrorType) {
    return new UpdateException(null, updateErrorType, null, null);
  }

  public static UpdateException of(
    FeedScopedId tripId,
    UpdateErrorType updateErrorType,
    int stopIndex
  ) {
    return new UpdateException(tripId, updateErrorType, stopIndex, null);
  }

  // Gives an updated exception with the specified dataSource
  public UpdateException withDataSource(String dataSource) {
    return new UpdateException(this.tripId, this.errorType, this.stopIndex, dataSource);
  }

  // Gives an updated exception with the specified tripId
  public UpdateException withTripId(FeedScopedId tripId) {
    return new UpdateException(tripId, this.errorType, this.stopIndex, this.producer);
  }

  @Nullable
  public Integer stopIndex() {
    return stopIndex;
  }

  public UpdateErrorType errorType() {
    return errorType;
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

  public UpdateError toError() {
    return new UpdateError(tripId, errorType, stopIndex, producer);
  }
}
