package org.opentripplanner.ext.trias.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.api.model.transit.UseFeedIdMapper;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class UseFeedIdResolverTest {

  private static final FeedScopedIdMapper RESOLVER = new UseFeedIdMapper();

  @Test
  void parse() {
    var id = RESOLVER.parse("aaa:bbb");
    assertEquals(new FeedScopedId("aaa", "bbb"), id);
  }

  @Test
  void shouldThrowInvalidArgumentException_whenIdIsInvalid() {
    assertThrows(IllegalArgumentException.class, () -> RESOLVER.parse("invalid"));
  }

  @Test
  void tostring() {
    var id = RESOLVER.mapToApi(new FeedScopedId("aaa", "bbb"));
    assertEquals("aaa:bbb", id);
  }
}
