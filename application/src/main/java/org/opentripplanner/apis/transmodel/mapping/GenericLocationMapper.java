package org.opentripplanner.apis.transmodel.mapping;

import java.util.Map;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GenericLocationMapper {

  private final FeedScopedIdMapper idMapper;

  GenericLocationMapper(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  /**
   * Maps a GraphQL Location input type to a GenericLocation
   */
  GenericLocation toGenericLocation(Map<String, Object> m) {
    Map<String, Object> coordinates = (Map<String, Object>) m.get("coordinates");
    Double lat = null;
    Double lon = null;
    if (coordinates != null) {
      lat = (Double) coordinates.get("latitude");
      lon = (Double) coordinates.get("longitude");
    }

    String placeRef = (String) m.get("place");
    FeedScopedId stopId = idMapper.parseNullSafe(placeRef).orElse(null);
    String name = (String) m.get("name");
    name = name == null ? "" : name;

    return new GenericLocation(name, stopId, lat, lon);
  }
}
