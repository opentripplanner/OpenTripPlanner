package org.opentripplanner.gtfs.mapping;

import javax.annotation.Nullable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Responsible for creating feed-scoped from OBA's {@code AgencyAndId} or strings.
 **/
class IdFactory {

  private final String feedId;

  IdFactory(String feedId) {
    this.feedId = feedId;
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  FeedScopedId createId(@Nullable AgencyAndId id) {
    return id == null ? null : new FeedScopedId(feedId, id.getId());
  }

  FeedScopedId createId(@Nullable String id) {
    return id == null ? null : new FeedScopedId(feedId, id);
  }
}
