package org.opentripplanner._support.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;

public class Polygons {

  public static final Geometry BERLIN = GeometryUtils
    .getGeometryFactory()
    .createPolygon(
      new Coordinate[] {
        new Coordinate(52.616841, 13.224692810),
        new Coordinate(52.6168419, 13.224692810),
        new Coordinate(52.3915238, 13.646107734),
        new Coordinate(52.616841, 13.646107734),
        new Coordinate(52.616841, 13.224692810),
      }
    );
}
