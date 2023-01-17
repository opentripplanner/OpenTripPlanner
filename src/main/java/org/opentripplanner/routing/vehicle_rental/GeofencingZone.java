package org.opentripplanner.routing.vehicle_rental;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record GeofencingZone(
  FeedScopedId id,
  Geometry geometry,
  boolean dropOffBanned,
  boolean traversalBanned
) {
  public boolean hasRestriction() {
    return dropOffBanned || traversalBanned;
  }
  public boolean isBusinessArea() {
    return !dropOffBanned && !traversalBanned;
  }
}
