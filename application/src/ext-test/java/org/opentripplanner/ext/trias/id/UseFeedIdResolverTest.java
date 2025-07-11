package org.opentripplanner.ext.trias.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
  void shouldResolveToNull_whenNull() {
    FeedScopedId feedScopedId = RESOLVER.parseNullSafe(null);
    assertNull(feedScopedId);
  }

  @Test
  void shouldResolveToNull_whenEmpty() {
    FeedScopedId feedScopedId = RESOLVER.parseNullSafe("");
    assertNull(feedScopedId);
  }

  @Test
  void shouldResolveToNull_whenBlank() {
    // this test comes from the TransitIdMapperTest, see also todo in FeedScopeId
    FeedScopedId feedScopedId = RESOLVER.parseNullSafe(" ");
    assertNull(feedScopedId);
  }

  @Test
  void shouldThrowInvalidArgumentException_whenIdIsInvalid() {
    assertThrows(IllegalArgumentException.class, () -> RESOLVER.parseNullSafe("invalid"));
  }

  @Test
  void tostring() {
    var id = RESOLVER.toString(new FeedScopedId("aaa", "bbb"));
    assertEquals("aaa:bbb", id);
  }
}
