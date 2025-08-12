package org.opentripplanner.ext.trias.id;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opentripplanner.api.model.transit.HideFeedIdMapper;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class HideFeedIdResolverTest {

  private static final HideFeedIdMapper RESOLVER = new HideFeedIdMapper("aaa");

  @Test
  void parse() {
    var id = RESOLVER.parse("bbb");
    assertEquals(new FeedScopedId("aaa", "bbb"), id);
  }

  @Test
  void tostring() {
    var id = RESOLVER.mapToApi(new FeedScopedId("aaa", "bbb"));
    assertEquals("bbb", id);
  }
}
