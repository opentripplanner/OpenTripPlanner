package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.FeedId;

/** Responsible for mapping GTFS AgencyAndId into the OTP model. */
public class AgencyAndIdMapper {

    /** Map from GTFS to OTP model, {@code null} safe.  */
    public static FeedId mapAgencyAndId(org.onebusaway.gtfs.model.AgencyAndId id) {
        return id == null ? null : new FeedId(id.getAgencyId(), id.getId());
    }
}
