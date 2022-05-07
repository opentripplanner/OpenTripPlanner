package org.opentripplanner.transit.model.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FeedScopedIdTest {

  @Test
  void ofNullable() {
    assertEquals(new FeedScopedId("FEED", "ID"), FeedScopedId.ofNullable("FEED", "ID"));
    assertNull(FeedScopedId.ofNullable("FEED", null));
  }
}
