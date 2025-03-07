package org.opentripplanner.framework.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

public final class DirectionUtils {

  private DirectionUtils() {}

  /**
   * Returns the approximate azimuth from coordinate A to B in decimal degrees clockwise from North,
   * in the range (-180° to +180°). The computation is exact for small delta between A and B.
   */
  public static double getAzimuth(Coordinate a, Coordinate b) {
    double cosLat = Math.cos(Math.toRadians((a.y + b.y) / 2.0));
    double dY = (b.y - a.y); // in degrees, we do not care about the units
    double dX = (b.x - a.x) * cosLat; // same
    if (Math.abs(dX) < 1e-10 && Math.abs(dY) < 1e-10) {
      return 180;
    }
    return Math.toDegrees(Math.atan2(dX, dY));
  }

  /**
   * Computes the angle of the last segment of a LineString or MultiLineString in radians clockwise
   * from North in the range (-PI, PI).
   *
   * @param geometry a LineString or a MultiLineString
   */
  public static double getLastAngle(Geometry geometry) {
    LineString line;
    if (geometry instanceof MultiLineString) {
      line = (LineString) geometry.getGeometryN(geometry.getNumGeometries() - 1);
    } else {
      assert geometry instanceof LineString;
      line = (LineString) geometry;
    }
    int numPoints = line.getNumPoints();
    Coordinate coord0 = line.getCoordinateN(numPoints - 2);
    Coordinate coord1 = line.getCoordinateN(numPoints - 1);
    int i = numPoints - 3;
    int minDistance = 10; // Meters
    while (SphericalDistanceLibrary.fastDistance(coord0, coord1) < minDistance && i >= 0) {
      coord0 = line.getCoordinateN(i--);
    }

    double az = getAzimuth(coord0, coord1);
    return (az * Math.PI) / 180;
  }

  /**
   * Computes the angle of the first segment of a LineString or MultiLineString in radians clockwise
   * from North in the range (-PI, PI).
   *
   * @param geometry a LineString or a MultiLineString
   */
  public static double getFirstAngle(Geometry geometry) {
    LineString line;
    if (geometry instanceof MultiLineString) {
      line = (LineString) geometry.getGeometryN(0);
    } else {
      assert geometry instanceof LineString;
      line = (LineString) geometry;
    }

    Coordinate coord0 = line.getCoordinateN(0);
    Coordinate coord1 = line.getCoordinateN(1);
    int i = 2;
    int minDistance = 10; // Meters
    while (
      SphericalDistanceLibrary.fastDistance(coord0, coord1) < minDistance && i < line.getNumPoints()
    ) {
      coord1 = line.getCoordinateN(i++);
    }

    double az = getAzimuth(coord0, coord1);
    return (az * Math.PI) / 180;
  }
}
