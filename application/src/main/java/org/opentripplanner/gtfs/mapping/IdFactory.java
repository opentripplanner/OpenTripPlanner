package org.opentripplanner.gtfs.mapping;

import javax.annotation.Nullable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/** Responsible for mapping GTFS AgencyAndId into the OTP model. */
class IdFactory {

  private final String feedId;

  IdFactory(String feedId) {
    this.feedId = feedId;
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  FeedScopedId toId(@Nullable AgencyAndId id) {
    return id == null ? null : new FeedScopedId(feedId, id.getId());
  }

  FeedScopedId id(@Nullable String id) {
    return id == null ? null : new FeedScopedId(feedId, id);
  }
}
