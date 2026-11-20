package org.opentripplanner.transit;

import java.util.List;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Interface for resolving stops, entrances, and the coordinates and child stops of stations and
 * other stop groupings.
 * <p>
 * This is useful if you don't want to add a direct dependency on a big service or repository to
 * a component. Instead, you can write small adapter implementations.
 */
public interface SiteResolver extends EntranceResolver {
  RegularStop getStop(FeedScopedId id);
  StopLocation getStopLocation(FeedScopedId id);
  Entrance getEntrance(FeedScopedId id);
  List<FeedScopedId> findStopOrChildIds(FeedScopedId id);

  /**
   * Finds the coordinate of a {@link GenericLocation}. An explicit coordinate on the location
   * takes precedence. Otherwise, the coordinate of the stop is used, or the centroid of the
   * station or group of stations if the location refers to one. Returns empty if the coordinate
   * cannot be resolved.
   */
  Optional<WgsCoordinate> findCoordinate(GenericLocation location);
}
