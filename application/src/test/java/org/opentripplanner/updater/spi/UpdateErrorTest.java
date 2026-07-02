package org.opentripplanner.updater.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;

class UpdateErrorTest {

  private static final FeedScopedId TRIP_ID = new FeedScopedId("F", "Trip1");

  @Test
  void resolvedTripIdIsPreferred() {
    var error = new UpdateError(TRIP_ID, UpdateErrorType.TRIP_NOT_FOUND, null, null, "ignored-ref");
    assertEquals("F:Trip1", error.debugId());
  }

  @Test
  void fallBackToTripReferenceWhenTripIdIsMissing() {
    var error = new UpdateError(
      null,
      UpdateErrorType.NOT_MONITORED,
      null,
      null,
      "SJ:1 (2026-06-29)"
    );
    assertEquals("SJ:1 (2026-06-29)", error.debugId());
  }

  @Test
  void noTripIdWhenNeitherIsAvailable() {
    var error = new UpdateError(null, UpdateErrorType.NOT_MONITORED, null, null, null);
    assertEquals("no trip id", error.debugId());
  }

  @Test
  void stopIndexIsAppendedToResolvedTripId() {
    var error = new UpdateError(TRIP_ID, UpdateErrorType.INVALID_ARRIVAL_TIME, 3, null, null);
    assertEquals("F:Trip1{stopIndex=3}", error.debugId());
  }

  @Test
  void stopIndexIsAppendedToTripReference() {
    var error = new UpdateError(null, UpdateErrorType.INVALID_ARRIVAL_TIME, 3, null, "SJ:1");
    assertEquals("SJ:1{stopIndex=3}", error.debugId());
  }
}
