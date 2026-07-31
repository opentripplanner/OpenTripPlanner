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
  private final Integer stopPosition;

  /// A best-effort trip identifier used for logging when no resolved [FeedScopedId] is available.
  @Nullable
  private final String tripReference;

  private UpdateException(
    @Nullable FeedScopedId tripId,
    UpdateErrorType errorType,
    @Nullable Integer stopPosition,
    @Nullable String tripReference
  ) {
    this.tripId = tripId;
    this.errorType = errorType;
    this.stopPosition = stopPosition;
    this.tripReference = tripReference;
  }

  public static UpdateException of(@Nullable FeedScopedId tripId, UpdateErrorType errorType) {
    return new UpdateException(tripId, errorType, null, null);
  }

  public static UpdateException ofStopPosition(UpdateErrorType updateErrorType, int stopPosition) {
    return new UpdateException(null, updateErrorType, stopPosition, null);
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
    int stopPosition
  ) {
    return new UpdateException(tripId, updateErrorType, stopPosition, null);
  }

  /// Gives an updated exception with the specified tripId
  public UpdateException withTripId(FeedScopedId tripId) {
    return new UpdateException(tripId, this.errorType, this.stopPosition, this.tripReference);
  }

  /// Gives an updated exception with a best-effort trip identifier for logging, used when the trip
  /// could not be resolved to a [FeedScopedId].
  public UpdateException withTripReference(@Nullable String tripReference) {
    return new UpdateException(this.tripId, this.errorType, this.stopPosition, tripReference);
  }

  /// The position of the GTFS-RT stop time update or SIRI call in the list of updates, which
  /// does not necessarily correspond to the stop position in pattern.
  @Nullable
  public Integer stopPosition() {
    return stopPosition;
  }

  public UpdateErrorType errorType() {
    return errorType;
  }

  public UpdateError toError() {
    return new UpdateError(tripId, errorType, stopPosition, null, tripReference);
  }

  public UpdateError toError(String producer) {
    return new UpdateError(tripId, errorType, stopPosition, producer, tripReference);
  }
}
