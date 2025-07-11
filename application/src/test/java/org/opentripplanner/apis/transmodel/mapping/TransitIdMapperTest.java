package org.opentripplanner.apis.transmodel.mapping;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class TransitIdMapperTest {

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
