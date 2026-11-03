package org.opentripplanner.transit.service;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.street.geometry.WgsCoordinate;

public class GenericLocationHelper {

  /**
   * Finds the coordinate of a generic location, which may be given as a stop or station id without
   * coordinates. An explicit coordinate takes precedence; otherwise the coordinate of the stop is
   * used, or the centroid of the station or group of stations if the id refers to one. Returns
   * {@code null} if the location cannot be resolved.
   */
  @Nullable
  public static WgsCoordinate findCoordinate(
    TransitService transitService,
    GenericLocation location
  ) {
    if (location.wgsCoordinate() != null) {
      return location.wgsCoordinate();
    }
    var id = location.stopId();
    if (id == null) {
      return null;
    }
    var stop = transitService.getStopLocation(id);
    if (stop != null) {
      return stop.getCoordinate();
    }
    var group = transitService.getStopLocationsGroup(id);
    if (group != null) {
      return group.getCoordinate();
    }
    return null;
  }
}
