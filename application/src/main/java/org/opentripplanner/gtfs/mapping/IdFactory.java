package org.opentripplanner.gtfs.mapping;

import java.util.Objects;
import javax.annotation.Nullable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Responsible for creating feed-scoped from OBA's {@code AgencyAndId} or strings.
 **/
class IdFactory {

  private final String feedId;

  IdFactory(String feedId) {
    this.feedId = Objects.requireNonNull(feedId);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  @Nullable
  FeedScopedId createId(@Nullable AgencyAndId id) {
    return id == null ? null : new FeedScopedId(feedId, id.getId());
  }

  /**
   * Maps from OBA strings to feed-scoped ids. Parameter must not be null.
   */
  FeedScopedId createId(String id) {
    Objects.requireNonNull(id, "id must not be null");
    return new FeedScopedId(feedId, id);
  }

  /**
   * Maps from OBA strings to feed-scoped ids. Parameter may be null and returns null in such a case.
   */
  @Nullable
  FeedScopedId createNullableId(@Nullable String id) {
    return id == null ? null : new FeedScopedId(feedId, id);
  }
}
