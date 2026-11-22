package org.opentripplanner.service.shape.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;

class FeedPartitionedMapTest {

  private static final FeedScopedId F_KEY = new FeedScopedId("F", "key");
  private static final FeedScopedId G_KEY = new FeedScopedId("G", "key");
  private static final String VALUE = "value";
  private static final String OTHER_VALUE = "other value";

  private final FeedPartitionedMap<FeedScopedId, String> map = new FeedPartitionedMap<>();

  @Test
  void writesAreVisibleImmediately() {
    map.put("F", F_KEY, VALUE);

    assertEquals(VALUE, map.get("F", F_KEY));
    assertNull(map.get("G", G_KEY));
  }

  @Test
  void frozenCopyIsNotAffectedByLaterWritesOrClears() {
    map.put("F", F_KEY, VALUE);
    var frozen = map.freeze();

    map.put("F", F_KEY, OTHER_VALUE);
    map.put("G", G_KEY, OTHER_VALUE);
    assertEquals(VALUE, frozen.get("F", F_KEY));
    assertNull(frozen.get("G", G_KEY));

    map.clear("F");
    assertEquals(VALUE, frozen.get("F", F_KEY));
  }

  @Test
  void freezeAfterClearRemovesTheFeedAndKeepsTheOthers() {
    map.put("F", F_KEY, VALUE);
    map.put("G", G_KEY, OTHER_VALUE);
    map.freeze();

    map.clear("F");
    var frozen = map.freeze();

    assertNull(frozen.get("F", F_KEY));
    assertEquals(OTHER_VALUE, frozen.get("G", G_KEY));
  }

  @Test
  void unchangedFeedIsKeptWhenAnotherFeedChanges() {
    map.put("F", F_KEY, VALUE);
    map.freeze();

    map.put("G", G_KEY, OTHER_VALUE);
    var frozen = map.freeze();

    assertEquals(VALUE, frozen.get("F", F_KEY));
    assertEquals(OTHER_VALUE, frozen.get("G", G_KEY));
  }

  @Test
  void frozenCopyCanNotBeModified() {
    var frozen = map.freeze();

    assertThrows(IllegalStateException.class, () -> frozen.put("F", F_KEY, VALUE));
    assertThrows(IllegalStateException.class, () -> frozen.clear("F"));
    assertThrows(IllegalStateException.class, frozen::freeze);
  }
}
