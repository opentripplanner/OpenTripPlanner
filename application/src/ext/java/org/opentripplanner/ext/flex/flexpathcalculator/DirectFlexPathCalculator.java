package org.opentripplanner.ext.flex.flexpathcalculator;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Calculated driving times and distance based on direct distance and fixed average driving speed.
 */
public class DirectFlexPathCalculator implements FlexPathCalculator {

  public static final double FLEX_SPEED = 8.0;

  private static final int DIRECT_EXTRA_TIME = 5 * 60;

  private final double flexSpeed;

  public DirectFlexPathCalculator() {
    this.flexSpeed = FLEX_SPEED;
  }

  @Override
  public FlexPath calculateFlexPath(
    Vertex fromv,
    Vertex tov,
    int boardStopPosition,
    int alightStopPosition
  ) {
    double distance = SphericalDistanceLibrary.distance(fromv.getCoordinate(), tov.getCoordinate());
    LineString geometry = GeometryUtils.getGeometryFactory()
      .createLineString(new Coordinate[] { fromv.getCoordinate(), tov.getCoordinate() });

    return new FlexPath((int) distance, (int) (distance / flexSpeed) + DIRECT_EXTRA_TIME, () ->
      geometry
    );
  }
}
