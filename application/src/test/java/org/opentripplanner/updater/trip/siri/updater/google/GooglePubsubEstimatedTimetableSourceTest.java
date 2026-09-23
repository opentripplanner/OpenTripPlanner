package org.opentripplanner.updater.trip.siri.updater.google;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class GooglePubsubEstimatedTimetableSourceTest {

  @Test
  void dataInitializationUrlIsOptional() {
    var source = new GooglePubsubEstimatedTimetableSource(
      null,
      Duration.ofSeconds(30),
      Duration.ofSeconds(30),
      "subscription-project",
      "topic-project",
      "topic"
    );
    assertFalse(source.isPrimed());
  }
}
