package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiTripSearchMetadata;
import org.opentripplanner.routing.api.response.TripSearchMetadata;

import java.time.Instant;

public class TripSearchMetadataMapper {

    public static ApiTripSearchMetadata mapTripSearchMetadata(TripSearchMetadata domain) {
        if(domain == null) { return null; }

        ApiTripSearchMetadata api = new ApiTripSearchMetadata();
        api.searchWindowUsed = (int)domain.searchWindowUsed.toSeconds();
        api.nextDateTime = mapInstantToMs(domain.nextDateTime);
        api.prevDateTime = mapInstantToMs(domain.prevDateTime);
        return api;
    }

    private static Long mapInstantToMs(Instant instant) {
        return instant == null ? null : instant.toEpochMilli();
    }
}
