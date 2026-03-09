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
  @Nullable Integer stopIndex,
  @Nullable String producer
) {
  public String debugId() {
    if (tripId == null) {
      return "no trip id";
    } else if (stopIndex == null) {
      return tripId.toString();
    } else {
      return "%s{stopIndex=%s}".formatted(tripId, stopIndex);
    }
  }
}
