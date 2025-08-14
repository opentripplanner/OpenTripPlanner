package org.opentripplanner.api.model.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class DefaultFeedIdMapperTest {

  private static final FeedScopedIdMapper MAPPER = new DefaultFeedIdMapper();

  @Test
  void parse() {
    var id = MAPPER.parse("aaa:bbb");
    assertEquals(new FeedScopedId("aaa", "bbb"), id);
  }

  @Test
  void shouldThrowInvalidArgumentException_whenIdIsInvalid() {
    assertThrows(IllegalArgumentException.class, () -> MAPPER.parse("invalid"));
  }

  @Test
  void tostring() {
    var id = MAPPER.mapToApi(new FeedScopedId("aaa", "bbb"));
    assertEquals("aaa:bbb", id);
  }
}
