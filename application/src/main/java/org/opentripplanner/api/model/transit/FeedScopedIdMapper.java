package org.opentripplanner.api.model.transit;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Handles mapping from external IDs into feed-scoped ones.
 */
public interface FeedScopedIdMapper {
  FeedScopedId parse(String id);

  /**
   * @param id a string representation of the id that should be parsed. May be <code>null</code> or
   *           blank
   * @return <code>null</code> if the input id is <code>null</code> or blank, otherwise a
   * <code>FeedScopedId</code>
   */
  @Nullable
  default FeedScopedId parseNullSafe(@Nullable String id) {
    if (id == null || id.isBlank()) {
      return null;
    }
    return parse(id);
  }

  /**
   * @param ids a collection of string representations of the ids that should be parsed. May contain
   *            <code>null</code> or blank values
   * @return a list of <code>FeedScopedId</code>. Any <code>null</code> or blank values in the input
   * collection are filtered out.
   */
  default List<FeedScopedId> parseList(Collection<String> ids) {
    return ids.stream().map(this::parseNullSafe).filter(Objects::nonNull).toList();
  }

  /**
   * @param ids a collection of string representations of the ids that should be parsed. May be
   *            <code>null</code> and may contain <code>null</code> or blank values
   * @return empty list if the input collection is <code>null</code>, otherwise a list of
   * <code>FeedScopedId</code>. Any <code>null</code> or blank values in the input collection are
   * filtered out.
   * @deprecated This method should no longer be used. Use
   * {@link FeedScopedIdMapper#parseList(Collection)} instead and handle null cases at call site
   */
  @Deprecated
  default List<FeedScopedId> parseListNullSafe(@Nullable Collection<String> ids) {
    if (ids == null) {
      return Collections.emptyList();
    }
    return ids.stream().map(this::parseNullSafe).filter(Objects::nonNull).toList();
  }

  String mapToApi(FeedScopedId id);
}
