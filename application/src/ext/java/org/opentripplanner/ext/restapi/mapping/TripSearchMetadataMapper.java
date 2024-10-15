package org.opentripplanner.ext.restapi.mapping;

import java.time.Instant;
import org.opentripplanner.ext.restapi.model.ApiTripSearchMetadata;
import org.opentripplanner.routing.api.response.TripSearchMetadata;

public class TripSearchMetadataMapper {

  public static ApiTripSearchMetadata mapTripSearchMetadata(TripSearchMetadata domain) {
    if (domain == null) {
      return null;
    }

    ApiTripSearchMetadata api = new ApiTripSearchMetadata();
    api.searchWindowUsed = (int) domain.searchWindowUsed.toSeconds();
    api.nextDateTime = mapInstantToMs(domain.nextDateTime);
    api.prevDateTime = mapInstantToMs(domain.prevDateTime);
    return api;
  }

  private static Long mapInstantToMs(Instant instant) {
    return instant == null ? null : instant.toEpochMilli();
  }
}
