package org.opentripplanner.api.model.transit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class HideFeedIdMapperTest {

  private static final HideFeedIdMapper MAPPER = new HideFeedIdMapper("aaa");

  @Test
  void parse() {
    var id = MAPPER.parse("bbb");
    assertEquals(new FeedScopedId("aaa", "bbb"), id);
  }

  @Test
  void tostring() {
    var id = MAPPER.mapToApi(new FeedScopedId("aaa", "bbb"));
    assertEquals("bbb", id);
  }
}
