package org.opentripplanner.apis.transmodel.mapping;

import java.util.Map;
import org.opentripplanner.ext.trias.id.IdResolver;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GenericLocationMapper {

  private final IdResolver idResolver;

  GenericLocationMapper(IdResolver idResolver) {
    this.idResolver = idResolver;
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
    FeedScopedId stopId = idResolver.parseNullSafe(placeRef);
    String name = (String) m.get("name");
    name = name == null ? "" : name;

    return new GenericLocation(name, stopId, lat, lon);
  }
}
