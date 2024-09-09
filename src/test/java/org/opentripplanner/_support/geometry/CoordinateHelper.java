package org.opentripplanner._support.geometry;

import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.geometry.WgsCoordinate;

/**
 * Helper class for creating coordinates at reasonable distances from each other.
 */
public class CoordinateHelper {

  WgsCoordinate coord;

  public CoordinateHelper(double lat, double lon) {
    this(new WgsCoordinate(lat, lon));
  }

  public CoordinateHelper(WgsCoordinate coord) {
    this.coord = coord;
  }

  public WgsCoordinate get() {
    return coord;
  }

  public WgsCoordinate moveMeters(double latMeters, double lonMeters) {
    var degreesLat = SphericalDistanceLibrary.metersToDegrees(latMeters);
    var degreesLon = SphericalDistanceLibrary.metersToLonDegrees(lonMeters, coord.latitude());
    return coord.add(degreesLat, degreesLon);
  }

  public WgsCoordinate east(double meters) {
    return moveMeters(0, meters);
  }

  public WgsCoordinate west(double meters) {
    return moveMeters(0, -meters);
  }

  public WgsCoordinate north(double meters) {
    return moveMeters(meters, 0);
  }

  public WgsCoordinate south(double meters) {
    return moveMeters(-meters, 0);
  }
}
