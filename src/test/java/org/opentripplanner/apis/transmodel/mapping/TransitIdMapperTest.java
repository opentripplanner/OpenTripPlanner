package org.opentripplanner.apis.transmodel.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class TransitIdMapperTest {

  private static final String FEED_ID = "xxx";
  private static final String ID = "yyy";

  @Test
  void testMapValidId() {
    TransitIdMapper.clearFixedFeedId();
    FeedScopedId mappedID = TransitIdMapper.mapIDToDomain(FEED_ID + ":" + ID);
    assertNotNull(mappedID);
    assertEquals(FEED_ID, mappedID.getFeedId());
    assertEquals(ID, mappedID.getId());
  }

  @Test
  void testMapInvalidId() {
    TransitIdMapper.clearFixedFeedId();
    assertThrows(IllegalArgumentException.class, () -> TransitIdMapper.mapIDToDomain("invalid"));
  }

  @Test
  void testMapNullId() {
    assertNull(TransitIdMapper.mapIDToDomain(null));
  }

  @Test
  void testMapEmptyId() {
    assertNull(TransitIdMapper.mapIDToDomain(""));
  }

  @Test
  void testMapBlankId() {
    assertNull(TransitIdMapper.mapIDToDomain(" "));
  }

  @Test
  void testMapNullCollectionOfIds() {
    assertNotNull(TransitIdMapper.mapIDsToDomainNullSafe(null));
  }

  @Test
  void testMapEmptyCollectionOfIds() {
    assertNotNull(TransitIdMapper.mapIDsToDomainNullSafe(Set.of()));
  }

  @Test
  void testMapCollectionOfNullIds() {
    List<FeedScopedId> mappedIds = TransitIdMapper.mapIDsToDomainNullSafe(
      Arrays.asList(new String[] { null })
    );
    assertNotNull(mappedIds);
    assertTrue(mappedIds.isEmpty());
  }

  @Test
  void testMapCollectionOfEmptyIds() {
    List<FeedScopedId> mappedIds = TransitIdMapper.mapIDsToDomainNullSafe(List.of(""));
    assertNotNull(mappedIds);
    assertTrue(mappedIds.isEmpty());
  }
}
