package org.opentripplanner.api.model.transit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class FeedScopedIdMapperTest {

  private static final FeedScopedIdMapper SUBJECT = new IdResolverTestImpl();

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
      assertNotNull(SUBJECT.parseListNullSafe(null));
    }

    @Test
    void shouldResolveToEmptyList_whenEmpty() {
      assertNotNull(SUBJECT.parseListNullSafe(Set.of()));
    }

    @Test
    void shouldResolveToEmptyList_whenOnlyNullElements() {
      List<FeedScopedId> mappedIds = SUBJECT.parseListNullSafe(
        Arrays.asList(new String[] { null })
      );
      assertNotNull(mappedIds);
      assertTrue(mappedIds.isEmpty());
    }

    @Test
    void shouldResolveToEmptyList_whenOnlyBlankElements() {
      List<FeedScopedId> mappedIds = SUBJECT.parseListNullSafe(List.of(""));
      assertNotNull(mappedIds);
      assertTrue(mappedIds.isEmpty());
    }
  }

  @Nested
  class ParseNullSafe {

    @Test
    void shouldResolveToNull_whenNull() {
      FeedScopedId feedScopedId = SUBJECT.parseNullSafe(null);
      assertNull(feedScopedId);
    }

    @Test
    void shouldResolveToNull_whenEmpty() {
      FeedScopedId feedScopedId = SUBJECT.parseNullSafe("");
      assertNull(feedScopedId);
    }

    @Test
    void shouldResolveToNull_whenBlank() {
      FeedScopedId feedScopedId = SUBJECT.parseNullSafe(" ");
      assertNull(feedScopedId);
    }
  }
}
