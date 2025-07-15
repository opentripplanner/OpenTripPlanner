package org.opentripplanner.ext.trias.id;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class HideFeedIdResolverTest {

  private static final HideFeedIdResolver RESOLVER = new HideFeedIdResolver("aaa");

  @Test
  void parse() {
    var id = RESOLVER.parse("bbb");
    assertEquals(new FeedScopedId("aaa", "bbb"), id);
  }

  @Test
  void tostring() {
    var id = RESOLVER.toString(new FeedScopedId("aaa", "bbb"));
    assertEquals("bbb", id);
  }
}
