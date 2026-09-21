package org.opentripplanner.updater.spi;

import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * Detailed information about a failure to apply a realtime update, for example for trips or vehicle
 * positions.
 */
public record UpdateError(
  @Nullable FeedScopedId tripId,
  UpdateErrorType errorType,
  @Nullable Integer stopPosition,
  @Nullable String producer,
  /**
   * A best-effort, human-readable trip identifier used for logging when the trip could not be
   * resolved to a {@link FeedScopedId}, for example because the failure was raised during parsing
   * or validation, before the trip was resolved. It typically holds the raw reference carried by
   * the realtime message.
   */
  @Nullable String tripReference
) {
  public String debugId() {
    var id = tripId != null ? tripId.toString() : tripReference;
    if (id == null) {
      return "no trip id";
    } else if (stopPosition == null) {
      return id;
    } else {
      return "%s{stopPosition=%s}".formatted(id, stopPosition);
    }
  }
}
