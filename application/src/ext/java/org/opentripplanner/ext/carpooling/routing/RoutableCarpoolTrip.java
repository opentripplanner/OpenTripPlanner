package org.opentripplanner.ext.carpooling.routing;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Pairs a {@link CarpoolTrip} with its {@link CarpoolCorridor}.
 */
public record RoutableCarpoolTrip(CarpoolTrip trip, CarpoolCorridor corridor) {
  public RoutableCarpoolTrip {
    Objects.requireNonNull(corridor, "corridor");
    if (corridor.waypoints().size() != trip.stops().size()) {
      throw new IllegalArgumentException(
        "Number of vertices (%d) does not match number of stops (%d)".formatted(
          corridor.waypoints().size(),
          trip.stops().size()
        )
      );
    }
  }

  /** The corridor's waypoints. */
  public List<Vertex> vertices() {
    return corridor.waypoints();
  }
}
