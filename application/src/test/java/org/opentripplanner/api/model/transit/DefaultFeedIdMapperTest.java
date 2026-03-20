package org.opentripplanner.api.model.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;

class DefaultFeedIdMapperTest {

  private static final FeedScopedIdMapper SUBJECT = new DefaultFeedIdMapper();

  @Test
  void parse() {
    var id = SUBJECT.parse("aaa:bbb");
    assertEquals(Optional.of(new FeedScopedId("aaa", "bbb")), id);
  }

  @Test
  void parseFail() {
    var id = SUBJECT.parse("aaa");
    assertEquals(Optional.empty(), id);
  }

  @Test
  void parseStrict() {
    var id = SUBJECT.parseStrict("aaa:bbb");
    assertEquals(new FeedScopedId("aaa", "bbb"), id);
  }

  @Test
  void parseStrictFail() {
    var e = assertThrows(IllegalArgumentException.class, () -> SUBJECT.parseStrict("invalid"));
    assertEquals("invalid feed-scoped-id: invalid", e.getMessage());
  }

  @Test
  void tostring() {
    var id = SUBJECT.mapToApi(new FeedScopedId("aaa", "bbb"));
    assertEquals("aaa:bbb", id);
  }
}
