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
    // in degrees, we do not care about the units
    double dY = (b.y - a.y);
    // same
    double dX = (b.x - a.x) * cosLat;
    if (Math.abs(dX) < 1e-10 && Math.abs(dY) < 1e-10) {
      return 180;
    }
    return Math.toDegrees(Math.atan2(dX, dY));
  }

  /**
   * Calculates the angular difference between two bearings in degrees.
   * <p>
   * Returns the smallest angle between the two bearings, accounting for the circular nature
   * of angles (e.g., 10° and -170° are only 20° apart, not 180°).
   * <p>
   * Works with any degree range (e.g., [0, 360) or [-180, 180]).
   *
   * @param bearing1 First bearing in degrees
   * @param bearing2 Second bearing in degrees
   * @return Smallest angular difference in degrees [0, 180]
   */
  public static double bearingDifference(double bearing1, double bearing2) {
    double diff = Math.abs(bearing1 - bearing2);

    // Take the smaller angle (handle wrap-around)
    if (diff > 180.0) {
      diff = 360.0 - diff;
    }

    return diff;
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
    int minDistance_m = 10;
    while (SphericalDistanceLibrary.fastDistance(coord0, coord1) < minDistance_m && i >= 0) {
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
    int minDistance_m = 10;
    while (
      SphericalDistanceLibrary.fastDistance(coord0, coord1) < minDistance_m &&
      i < line.getNumPoints()
    ) {
      coord1 = line.getCoordinateN(i++);
    }

    double az = getAzimuth(coord0, coord1);
    return (az * Math.PI) / 180;
  }
}
