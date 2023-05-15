package org.opentripplanner.ext.geocoder;

import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;

/**
 * A result of a fuzzy stop cluster geocoding search. A cluster is defined as a group of stops that
 * are related to one another.
 * <p>
 * Specifically this means that:
 * <p>
 *  - if a stop has a parent station only the parent is returned
 *  - if stops are closer than 10 meters to each and have an identical name, only one is returned
 */
record StopCluster(FeedScopedId id, @Nullable String code, String name, Coordinate coordinate) {
  public static StopCluster of(StopLocationsGroup g) {
    return new StopCluster(
      g.getId(),
      null,
      g.getName().toString(),
      toCoordinate(g.getCoordinate())
    );
  }

  static Optional<StopCluster> of(StopLocation sl) {
    return Optional
      .ofNullable(sl.getName())
      .map(name ->
        new StopCluster(sl.getId(), sl.getCode(), name.toString(), toCoordinate(sl.getCoordinate()))
      );
  }

  private static Coordinate toCoordinate(WgsCoordinate c) {
    return new Coordinate(c.latitude(), c.longitude());
  }

  /**
   * Easily serializable version of a coordinate
   */
  public record Coordinate(double lat, double lon) {}
}
