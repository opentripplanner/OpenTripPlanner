package org.opentripplanner.framework.geometry;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

public abstract class SphericalDistanceLibrary {

  public static final double RADIUS_OF_EARTH_IN_KM = 6371.01;
  public static final double RADIUS_OF_EARTH_IN_M = RADIUS_OF_EARTH_IN_KM * 1000;

  // Max admissible lat/lon delta for approximated distance computation
  public static final double MAX_LAT_DELTA_DEG = 4.0;
  public static final double MAX_LON_DELTA_DEG = 4.0;

  // 1 / Max over-estimation error of approximated distance,
  // for delta lat/lon in given range
  public static final double MAX_ERR_INV = 0.999462;

  public static double distance(Coordinate from, Coordinate to) {
    return distance(from.y, from.x, to.y, to.x);
  }

  /**
   * @see SphericalDistanceLibrary#fastDistance(double, double, double, double)
   */
  public static double fastDistance(Coordinate from, Coordinate to) {
    return fastDistance(from.y, from.x, to.y, to.x);
  }

  /**
   * @see SphericalDistanceLibrary#fastDistance(double, double, double, double)
   */
  public static double fastDistance(Coordinate from, Coordinate to, double cosLat) {
    double dLat = toRadians(from.y - to.y);
    double dLon = toRadians(from.x - to.x) * cosLat;
    return RADIUS_OF_EARTH_IN_M * sqrt(dLat * dLat + dLon * dLon);
  }

  /**
   * Compute an (approximated) distance between a point and a linestring expressed in standard
   * geographical coordinates (lon, lat in degrees).
   *
   * @param point      The coordinates of the point (longitude, latitude degrees).
   * @param lineString The set of points representing the polyline, in the same coordinate system.
   * @return The (approximated) distance, in meters, between the point and the linestring.
   */
  public static double fastDistance(Coordinate point, LineString lineString) {
    // Transform in equirectangular projection on sphere of radius 1,
    // centered at point
    double lat = toRadians(point.y);
    double cosLat = cos(lat);
    double lon = toRadians(point.x) * cosLat;
    Point point2 = GeometryUtils.getGeometryFactory().createPoint(new Coordinate(lon, lat));
    LineString lineString2 = equirectangularProject(lineString, cosLat);
    return lineString2.distance(point2) * RADIUS_OF_EARTH_IN_M;
  }

  /**
   * Compute the length of a polyline
   *
   * @param lineString The polyline in (longitude, latitude degrees).
   * @return The length, in meters, of the linestring.
   */
  public static double length(LineString lineString) {
    double accumulatedMeters = 0;

    for (int i = 1; i < lineString.getNumPoints(); i++) {
      accumulatedMeters += distance(lineString.getCoordinateN(i - 1), lineString.getCoordinateN(i));
    }

    return accumulatedMeters;
  }

  /**
   * Compute the (approximated) length of a polyline
   *
   * @param lineString The polyline in (longitude, latitude degrees).
   * @return The (approximated) length, in meters, of the linestring.
   */
  public static double fastLength(LineString lineString) {
    // Warn: do not use LineString.getCentroid() as it is broken
    // for degenerated geometry (same first/last point).
    Coordinate[] coordinates = lineString.getCoordinates();
    double middleY = (coordinates[0].y + coordinates[coordinates.length - 1].y) / 2.0;
    double cosLat = cos(toRadians(middleY));
    return equirectangularProject(lineString, cosLat).getLength() * RADIUS_OF_EARTH_IN_M;
  }

  /**
   * Compute the (approximated) length of a polyline, with known cos(lat).
   *
   * @param lineString The polyline in (longitude, latitude degrees).
   * @return The (approximated) length, in meters, of the linestring.
   */
  public static double fastLength(LineString lineString, double cosLat) {
    return equirectangularProject(lineString, cosLat).getLength() * RADIUS_OF_EARTH_IN_M;
  }

  public static double distance(double lat1, double lon1, double lat2, double lon2) {
    return distance(lat1, lon1, lat2, lon2, RADIUS_OF_EARTH_IN_M);
  }

  /**
   * Compute an (approximated) distance in meters between two points, with a known cos(lat).
   * Be careful, this is approximated and never checks for the validity of input cos(lat).
   */
  public static double fastDistance(double lat1, double lon1, double lat2, double lon2) {
    return fastDistance(lat1, lon1, lat2, lon2, RADIUS_OF_EARTH_IN_M);
  }

  public static double distance(double lat1, double lon1, double lat2, double lon2, double radius) {
    // http://en.wikipedia.org/wiki/Great-circle_distance
    lat1 = toRadians(lat1); // Theta-s
    lon1 = toRadians(lon1); // Lambda-s
    lat2 = toRadians(lat2); // Theta-f
    lon2 = toRadians(lon2); // Lambda-f

    double deltaLon = lon2 - lon1;

    double y = sqrt(
      p2(cos(lat2) * sin(deltaLon)) +
      p2(cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon))
    );
    double x = sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(deltaLon);

    return radius * atan2(y, x);
  }

  /**
   * Approximated, fast and under-estimated equirectangular distance in meters between two points.
   * Works only for small delta lat/lon, fall-back on exact distance if not the case. See:
   * http://www.movable-type.co.uk/scripts/latlong.html
   */
  public static double fastDistance(
    double lat1,
    double lon1,
    double lat2,
    double lon2,
    double radius
  ) {
    if (abs(lat1 - lat2) > MAX_LAT_DELTA_DEG || abs(lon1 - lon2) > MAX_LON_DELTA_DEG) {
      return distance(lat1, lon1, lat2, lon2, radius);
    }
    double dLat = toRadians(lat2 - lat1);
    double dLon = toRadians(lon2 - lon1) * cos(toRadians((lat1 + lat2) / 2));
    return radius * sqrt(dLat * dLat + dLon * dLon) * MAX_ERR_INV;
  }

  /**
   * @param distanceMeters Distance in meters.
   * @return The number of degree for the given distance. For degrees latitude, this is nearly
   * correct. For degrees longitude, this is an overestimate because meridians converge toward the
   * poles.
   */
  public static double metersToDegrees(double distanceMeters) {
    return (360 * distanceMeters) / (2 * Math.PI * RADIUS_OF_EARTH_IN_M);
  }

  /**
   * @return the approximate number of meters for the given number of degrees latitude. If degrees
   * longitude are supplied, this is an overestimate anywhere off the equator because meridians
   * converge toward the poles.
   */
  public static double degreesLatitudeToMeters(double degreesLatitude) {
    return ((2 * Math.PI * RADIUS_OF_EARTH_IN_M) * degreesLatitude) / 360;
  }

  /**
   * @param distanceMeters Distance in meters.
   * @param latDeg         Latitude of center point, in degree.
   * @return The number of longitude degree for the given distance. This is a slight overestimate as
   * the number of degree of longitude for a given distance depends on the exact latitude.
   */
  public static double metersToLonDegrees(double distanceMeters, double latDeg) {
    double dLatDeg = (360 * distanceMeters) / (2 * Math.PI * RADIUS_OF_EARTH_IN_M);
    /*
     * The computation below ensure that minCosLat is the minimum value of cos(lat) for lat in
     * the range [lat-dLat, lat+dLat].
     */
    double minCosLat;
    if (latDeg > 0) {
      minCosLat = cos(toRadians(latDeg + dLatDeg));
    } else {
      minCosLat = cos(toRadians(latDeg - dLatDeg));
    }
    return dLatDeg / minCosLat;
  }

  /**
   * Approximately move a coordinate a given number of meters. This will fail if crossing the anti-
   * meridian or any of the poles.
   */
  public static WgsCoordinate moveMeters(
    WgsCoordinate coordinate,
    double latMeters,
    double lonMeters
  ) {
    var degreesLat = metersToDegrees(latMeters);
    var degreesLon = metersToLonDegrees(lonMeters, coordinate.latitude());
    return coordinate.add(degreesLat, degreesLon);
  }

  public static Envelope bounds(double lat, double lon, double latDistance, double lonDistance) {
    double radiusOfEarth = RADIUS_OF_EARTH_IN_M;

    double latRadians = toRadians(lat);
    double lonRadians = toRadians(lon);

    double lonRadius = cos(latRadians) * radiusOfEarth;

    double latOffset = latDistance / radiusOfEarth;
    double lonOffset = lonDistance / lonRadius;

    double latFrom = toDegrees(latRadians - latOffset);
    double latTo = toDegrees(latRadians + latOffset);

    double lonFrom = toDegrees(lonRadians - lonOffset);
    double lonTo = toDegrees(lonRadians + lonOffset);

    return new Envelope(new Coordinate(lonFrom, latFrom), new Coordinate(lonTo, latTo));
  }

  /**
   * Equirectangular project a polyline.
   *
   * @param cosLat cos(lat) of the projection center point.
   * @return The projected polyline. Coordinates in radians.
   */
  private static LineString equirectangularProject(LineString lineString, double cosLat) {
    Coordinate[] coords = lineString.getCoordinates();
    Coordinate[] coords2 = new Coordinate[coords.length];
    for (int i = 0; i < coords.length; i++) {
      coords2[i] = new Coordinate(toRadians(coords[i].x) * cosLat, toRadians(coords[i].y));
    }
    return GeometryUtils.getGeometryFactory().createLineString(coords2);
  }

  private static double p2(double a) {
    return a * a;
  }
}
