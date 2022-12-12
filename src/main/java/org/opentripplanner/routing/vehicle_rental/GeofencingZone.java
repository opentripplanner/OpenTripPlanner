package org.opentripplanner.routing.vehicle_rental;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record GeofencingZone(
  FeedScopedId id,
  Geometry geometry,
  boolean dropOffBanned,
  boolean passingThroughBanned
) {
  public boolean hasRestriction() {
    return dropOffBanned || passingThroughBanned;
  }
  public boolean isBusinessArea() {
    return !dropOffBanned && !passingThroughBanned;
  }
}
