package org.opentripplanner.updater.spi;

import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;

/// An exception for indicating various issues with realtime data. It can be thrown anywhere in the
/// handling code for a realtime update and is caught by the UpdateAdapters.
public class UpdateException extends RuntimeException {

  private final UpdateErrorType errorType;

  @Nullable
  private final FeedScopedId tripId;

  @Nullable
  private final Integer stopIndex;

  private UpdateException(
    @Nullable FeedScopedId tripId,
    UpdateErrorType errorType,
    @Nullable Integer stopIndex
  ) {
    this.tripId = tripId;
    this.errorType = errorType;
    this.stopIndex = stopIndex;
  }

  public static UpdateException of(FeedScopedId tripId, UpdateErrorType errorType) {
    return new UpdateException(tripId, errorType, null);
  }

  public static UpdateException ofStopIndex(UpdateErrorType updateErrorType, int stopIndex) {
    return new UpdateException(null, updateErrorType, stopIndex);
  }

  public static UpdateException noTripId(UpdateErrorType errorType) {
    return new UpdateException(null, errorType, null);
  }

  public static UpdateException of(UpdateErrorType updateErrorType) {
    return new UpdateException(null, updateErrorType, null);
  }

  public static UpdateException of(
    FeedScopedId tripId,
    UpdateErrorType updateErrorType,
    int stopIndex
  ) {
    return new UpdateException(tripId, updateErrorType, stopIndex);
  }

  /// Gives an updated exception with the specified tripId
  public UpdateException withTripId(FeedScopedId tripId) {
    return new UpdateException(tripId, this.errorType, this.stopIndex);
  }

  /// The index of the GTFS-RT stop time update or SIRI call in the list of updates, which 
  /// does not necessarily correspond to the stop position in pattern.
  @Nullable
  public Integer stopIndex() {
    return stopIndex;
  }

  public UpdateErrorType errorType() {
    return errorType;
  }

  public UpdateError toError() {
    return new UpdateError(tripId, errorType, stopIndex, null);
  }

  public UpdateError toError(String producer) {
    return new UpdateError(tripId, errorType, stopIndex, producer);
  }
}
