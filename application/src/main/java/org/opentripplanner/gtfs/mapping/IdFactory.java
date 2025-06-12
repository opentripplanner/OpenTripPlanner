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

  /**
   * Maps from OBA AgencyAndId to feed-scoped ids. Values must not be null.
   * @param entityName The name of the entity being mapped. Improves the error message being thrown
   *                   in case of invalid values.
   */
  FeedScopedId createId(AgencyAndId id, String entityName) {
    Objects.requireNonNull(id, "id of %s must not be null".formatted(entityName));
    return createId(id.getId(), entityName);
  }

  /**
   * Maps from OBA strings to feed-scoped ids. Parameter must not be null.
   * @param entityName The name of the entity being mapped. Improves the error message being thrown
   *                   in case of invalid values.
   */
  FeedScopedId createId(String id, String entityName) {
    Objects.requireNonNull(id, "id of %s must not be null".formatted(entityName));
    return new FeedScopedId(feedId, id);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  @Nullable
  FeedScopedId createNullableId(@Nullable AgencyAndId id) {
    return id == null ? null : new FeedScopedId(feedId, id.getId());
  }

  /**
   * Maps from OBA strings to feed-scoped ids. Parameter may be null and returns null in such a case.
   */
  @Nullable
  FeedScopedId createNullableId(@Nullable String id) {
    return id == null ? null : new FeedScopedId(feedId, id);
  }
}
