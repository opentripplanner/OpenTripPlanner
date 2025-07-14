package org.opentripplanner.ext.trias.id;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Handles mapping from external IDs into feed-scoped ones.
 */
public interface IdResolver {
  // ToDo: relocate to somewhere outside of trias package
  FeedScopedId parse(String id);

  default FeedScopedId parseNullSafe(@Nullable String id) {
    if (id == null || id.isBlank()) {
      return null;
    }
    return parse(id);
  }

  default List<FeedScopedId> parseListNullSafe(@Nullable Collection<String> ids) {
    if (ids == null) {
      return Collections.emptyList();
    }
    return ids.stream().map(this::parseNullSafe).filter(Objects::nonNull).toList();
  }

  String toString(FeedScopedId id);
}
