package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiTripSearchMetadata;
import org.opentripplanner.model.routing.TripSearchMetadata;

public class TripSearchMetadataMapper {

    public static ApiTripSearchMetadata mapTripSearchMetadata(TripSearchMetadata domain) {
        if(domain == null) { return null; }

        ApiTripSearchMetadata api = new ApiTripSearchMetadata();
        api.searchWindowUsed = (int)domain.searchWindowUsed.toSeconds();
        api.nextDateTime = domain.nextDateTime.toEpochMilli();
        api.prevDateTime = domain.prevDateTime.toEpochMilli();
        return api;
    }
}
