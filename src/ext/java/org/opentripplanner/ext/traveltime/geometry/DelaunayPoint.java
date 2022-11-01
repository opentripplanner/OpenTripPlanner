package org.opentripplanner.ext.traveltime.geometry;

import org.locationtech.jts.geom.Coordinate;

/**
 * A DelaunayPoint is the geometrical point of a node of the triangulation.
 *
 * @author laurent
 */
interface DelaunayPoint<TZ> {
  /**
   * @return The geometric location of this point.
   */
  Coordinate getCoordinates();

  /**
   * @return The Z value for this point.
   */
  TZ getZ();
}
