package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.GraphPathUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * A carpool trip together with OTP's routed travel duration for each of its baseline legs, one per
 * leg.
 */
public record RoutedCarpoolTrip(CarpoolTripWithVertices tripWithVertices, Duration[] legDurations) {
  public RoutedCarpoolTrip {
    int legs = tripWithVertices.vertices().size() - 1;
    if (legDurations.length != legs) {
      throw new IllegalArgumentException(
        "legDurations length (%d) must equal the number of legs (%d)".formatted(
          legDurations.length,
          legs
        )
      );
    }
  }

  public CarpoolTrip trip() {
    return tripWithVertices.trip();
  }

  public List<Vertex> vertices() {
    return tripWithVertices.vertices();
  }

  public List<WgsCoordinate> vertexCoordinates() {
    return tripWithVertices.vertexCoordinates();
  }

  /** Cumulative arrival time at each route point, with {@code stopDuration} at every stop. */
  public Duration[] cumulativeArrivals(Duration stopDuration) {
    return GraphPathUtils.calculateCumulativeDurations(legDurations, stopDuration);
  }
}
