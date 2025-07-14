package org.opentripplanner.ext.trias.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class UseFeedIdResolverTest {

  private static final IdResolver RESOLVER = new UseFeedIdResolver();

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
    var id = RESOLVER.toString(new FeedScopedId("aaa", "bbb"));
    assertEquals("aaa:bbb", id);
  }
}
