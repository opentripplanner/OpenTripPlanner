package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.FeedId;

public class AgencyAndIdMapper {
    public static FeedId mapAgencyAndId(org.onebusaway.gtfs.model.AgencyAndId id) {
        return id == null ? null : new FeedId(id.getAgencyId(), id.getId());
    }
}
