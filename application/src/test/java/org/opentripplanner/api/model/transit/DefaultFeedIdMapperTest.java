package org.opentripplanner.api.model.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class DefaultFeedIdMapperTest {

  private static final FeedScopedIdMapper SUBJECT = new DefaultFeedIdMapper();

  @Test
  void parse() {
    var id = SUBJECT.parse("aaa:bbb");
    assertEquals(new FeedScopedId("aaa", "bbb"), id);
  }

  @Test
  void shouldThrowInvalidArgumentException_whenIdIsInvalid() {
    var e = assertThrows(IllegalArgumentException.class, () -> SUBJECT.parse("invalid"));
    assertEquals("invalid feed-scoped-id: invalid", e.getMessage());
  }

  @Test
  void tostring() {
    var id = SUBJECT.mapToApi(new FeedScopedId("aaa", "bbb"));
    assertEquals("aaa:bbb", id);
  }
}
