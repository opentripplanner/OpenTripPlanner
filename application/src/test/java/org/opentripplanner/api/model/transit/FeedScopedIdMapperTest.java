package org.opentripplanner.api.model.transit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class FeedScopedIdMapperTest {

  private static final FeedScopedIdMapper SUBJECT = new IdMapperTestImpl();

  private static class IdMapperTestImpl implements FeedScopedIdMapper {

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
    void shouldResolveToEmpty_whenNull() {
      Optional<FeedScopedId> feedScopedId = SUBJECT.parseNullSafe(null);
      assertNotNull(feedScopedId);
      assertTrue(feedScopedId.isEmpty());
    }

    @Test
    void shouldResolveToEmpty_whenEmpty() {
      Optional<FeedScopedId> feedScopedId = SUBJECT.parseNullSafe("");
      assertNotNull(feedScopedId);
      assertTrue(feedScopedId.isEmpty());
    }

    @Test
    void shouldResolveToEmpty_whenBlank() {
      Optional<FeedScopedId> feedScopedId = SUBJECT.parseNullSafe(" ");
      assertNotNull(feedScopedId);
      assertTrue(feedScopedId.isEmpty());
    }
  }
}
