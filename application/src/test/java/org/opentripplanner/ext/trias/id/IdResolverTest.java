package org.opentripplanner.ext.trias.id;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class IdResolverTest {

  private static final FeedScopedIdMapper ID_RESOLVER = new IdResolverTestImpl();

  private static class IdResolverTestImpl implements FeedScopedIdMapper {

    @Override
    public FeedScopedId parse(String id) {
      return new FeedScopedId("FIXED", id);
    }

    @Override
    public String mapToApi(FeedScopedId feedScopedId) {
      throw new NotImplementedException("Not implemented");
    }
  }

  @Nested
  class ParseListNullSafe {

    @Test
    void shouldResolveToEmptyList_whenNull() {
      assertNotNull(ID_RESOLVER.parseListNullSafe(null));
    }

    @Test
    void shouldResolveToEmptyList_whenEmpty() {
      assertNotNull(ID_RESOLVER.parseListNullSafe(Set.of()));
    }

    @Test
    void shouldResolveToEmptyList_whenOnlyNullElements() {
      List<FeedScopedId> mappedIds = ID_RESOLVER.parseListNullSafe(
        Arrays.asList(new String[] { null })
      );
      assertNotNull(mappedIds);
      assertTrue(mappedIds.isEmpty());
    }

    @Test
    void shouldResolveToEmptyList_whenOnlyBlankElements() {
      List<FeedScopedId> mappedIds = ID_RESOLVER.parseListNullSafe(List.of(""));
      assertNotNull(mappedIds);
      assertTrue(mappedIds.isEmpty());
    }
  }

  @Nested
  class ParseNullSafe {

    @Test
    void shouldResolveToNull_whenNull() {
      FeedScopedId feedScopedId = ID_RESOLVER.parseNullSafe(null);
      assertNull(feedScopedId);
    }

    @Test
    void shouldResolveToNull_whenEmpty() {
      FeedScopedId feedScopedId = ID_RESOLVER.parseNullSafe("");
      assertNull(feedScopedId);
    }

    @Test
    void shouldResolveToNull_whenBlank() {
      FeedScopedId feedScopedId = ID_RESOLVER.parseNullSafe(" ");
      assertNull(feedScopedId);
    }
  }
}
