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

  /**
   * Categorizes the directional relationship between two bearings.
   */
  public enum DirectionalAlignment {
    /** Directions are very similar (within 30°) - ideal match */
    HIGHLY_ALIGNED,

    /** Directions are compatible (within 60°) - acceptable match */
    ALIGNED,

    /** Directions differ but not opposite (60-120°) - marginal */
    DIVERGENT,

    /** Directions are opposite or very different (>120°) - incompatible */
    OPPOSITE;

    public static DirectionalAlignment categorize(double bearingDifference) {
      if (bearingDifference <= 30.0) {
        return HIGHLY_ALIGNED;
      } else if (bearingDifference <= 60.0) {
        return ALIGNED;
      } else if (bearingDifference <= 120.0) {
        return DIVERGENT;
      } else {
        return OPPOSITE;
      }
    }
  }

  /**
   * Classifies the directional alignment between two journeys.
   *
   * @param tripStart Starting point of the trip
   * @param tripEnd Ending point of the trip
   * @param passengerStart Passenger's starting point
   * @param passengerEnd Passenger's ending point
   * @return The alignment category
   */
  public static DirectionalAlignment classify(
    WgsCoordinate tripStart,
    WgsCoordinate tripEnd,
    WgsCoordinate passengerStart,
    WgsCoordinate passengerEnd
  ) {
    double tripBearing = calculateBearing(tripStart, tripEnd);
    double passengerBearing = calculateBearing(passengerStart, passengerEnd);
    double difference = bearingDifference(tripBearing, passengerBearing);

    return DirectionalAlignment.categorize(difference);
  }

  /**
   * Checks if a passenger journey is directionally compatible with a carpool trip.
   *
   * @param tripStart Starting point of the carpool trip
   * @param tripEnd Ending point of the carpool trip
   * @param passengerStart Passenger's desired pickup location
   * @param passengerEnd Passenger's desired dropoff location
   * @param toleranceDegrees Maximum allowed bearing difference in degrees
   * @return true if directions are compatible, false otherwise
   */
  public static boolean isDirectionallyCompatible(
    WgsCoordinate tripStart,
    WgsCoordinate tripEnd,
    WgsCoordinate passengerStart,
    WgsCoordinate passengerEnd,
    double toleranceDegrees
  ) {
    double tripBearing = calculateBearing(tripStart, tripEnd);
    double passengerBearing = calculateBearing(passengerStart, passengerEnd);
    double difference = bearingDifference(tripBearing, passengerBearing);

    return difference <= toleranceDegrees;
  }

  /**
   * Checks if adding a new point maintains forward progress along a route.
   *
   * @param previous Previous point in the route
   * @param newPoint Point to be inserted
   * @param next Next point in the route
   * @param toleranceDegrees Maximum allowed deviation in degrees
   * @return true if insertion maintains forward progress, false if it causes backtracking
   */
  public static boolean maintainsForwardProgress(
    WgsCoordinate previous,
    WgsCoordinate newPoint,
    WgsCoordinate next,
    double toleranceDegrees
  ) {
    double intendedBearing = calculateBearing(previous, next);
    double bearingToNew = calculateBearing(previous, newPoint);
    double bearingFromNew = calculateBearing(newPoint, next);

    double deviationToNew = bearingDifference(intendedBearing, bearingToNew);
    double deviationFromNew = bearingDifference(intendedBearing, bearingFromNew);

    return deviationToNew <= toleranceDegrees && deviationFromNew <= toleranceDegrees;
  }

  /**
   * Classifies directional alignment using custom thresholds.
   *
   * @param bearingDifference The bearing difference in degrees
   * @param highlyAlignedThreshold Threshold for HIGHLY_ALIGNED (e.g., 30.0)
   * @param alignedThreshold Threshold for ALIGNED (e.g., 60.0)
   * @param divergentThreshold Threshold for DIVERGENT (e.g., 120.0)
   * @return The alignment category
   */
  public static DirectionalAlignment classify(
    double bearingDifference,
    double highlyAlignedThreshold,
    double alignedThreshold,
    double divergentThreshold
  ) {
    if (bearingDifference <= highlyAlignedThreshold) {
      return DirectionalAlignment.HIGHLY_ALIGNED;
    } else if (bearingDifference <= alignedThreshold) {
      return DirectionalAlignment.ALIGNED;
    } else if (bearingDifference <= divergentThreshold) {
      return DirectionalAlignment.DIVERGENT;
    } else {
      return DirectionalAlignment.OPPOSITE;
    }
  }
}
