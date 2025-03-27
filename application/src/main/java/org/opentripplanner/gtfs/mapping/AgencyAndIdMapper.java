package org.opentripplanner.gtfs.mapping;

import javax.annotation.Nullable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/** Responsible for mapping GTFS AgencyAndId into the OTP model. */
class AgencyAndIdMapper {

  /** Map from GTFS to OTP model, {@code null} safe. */
  public static FeedScopedId mapAgencyAndId(@Nullable AgencyAndId id) {
    return id == null ? null : new FeedScopedId(id.getAgencyId(), id.getId());
  }

}
