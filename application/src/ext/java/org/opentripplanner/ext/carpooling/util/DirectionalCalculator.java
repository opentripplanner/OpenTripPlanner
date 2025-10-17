package org.opentripplanner.ext.carpooling.util;

import org.opentripplanner.framework.geometry.WgsCoordinate;

/**
 * Calculates bearings and directional relationships between geographic coordinates.
 * <p>
 * Uses the Haversine formula for accurate bearing calculations on Earth's surface.
 * All bearings are in degrees [0, 360) where 0° = North, 90° = East, etc.
 */
public class DirectionalCalculator {

  /**
   * Calculates the initial bearing (forward azimuth) from one point to another.
   * <p>
   * Uses the Haversine formula for accurate bearing on Earth's surface.
   *
   * @param from Starting point
   * @param to Ending point
   * @return Bearing in degrees [0, 360), where 0° is North
   */
  public static double calculateBearing(WgsCoordinate from, WgsCoordinate to) {
    double lat1 = Math.toRadians(from.latitude());
    double lat2 = Math.toRadians(to.latitude());
    double lon1 = Math.toRadians(from.longitude());
    double lon2 = Math.toRadians(to.longitude());

    double dLon = lon2 - lon1;

    double y = Math.sin(dLon) * Math.cos(lat2);
    double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

    double bearing = Math.toDegrees(Math.atan2(y, x));

    // Normalize to [0, 360)
    return (bearing + 360.0) % 360.0;
  }

  /**
   * Calculates the angular difference between two bearings.
   * <p>
   * Returns the smallest angle between the two bearings, accounting for
   * the circular nature of bearings (e.g., 10° and 350° are only 20° apart).
   *
   * @param bearing1 First bearing in degrees [0, 360)
   * @param bearing2 Second bearing in degrees [0, 360)
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
}
