package org.opentripplanner.ext.carpooling.util;

import java.time.Duration;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetConstants;

/**
 * Provides fast, low-resolution travel time estimates based on beeline (straight-line) distances.
 * <p>
 * Used to reject trips whose waypoints are too far apart to be driven within the longest trip
 * that is modelled, and, through {@link #minDriveSeconds}, to bound the car searches and the
 * pre-filters of a request. The estimates are intentionally optimistic (lower bounds) so that valid
 * trips are never incorrectly rejected.
 * <p>
 * Formula: duration = beeline_distance / speed (in m/s)
 * <p>
 * No street route is shorter than the beeline, so dividing it by the fastest speed any street
 * can be driven yields a guaranteed lower bound on the real drive time, whatever the road geometry.
 */
public class BeelineEstimator {

  /**
   * Fallback speed for the no-arg constructor when no graph-derived speed is supplied
   * ({@link StreetConstants#DEFAULT_MAX_CAR_SPEED}, 40 m/s ≈ 144 km/h): dividing by anything slower
   * than the fastest street would over-estimate and discard feasible trips on fast roads.
   */
  public static final double DEFAULT_SPEED_MPS = StreetConstants.DEFAULT_MAX_CAR_SPEED;

  private final double speed;

  /**
   * Creates estimator with the default speed.
   */
  public BeelineEstimator() {
    this(DEFAULT_SPEED_MPS);
  }

  /**
   * @param speed Travel speed in meters per second; must be at least the fastest speed a car can
   *              reach in the street model to preserve the lower-bound guarantee
   */
  public BeelineEstimator(double speed) {
    if (speed <= 0) {
      throw new IllegalArgumentException("speed must be positive (got " + speed + ")");
    }
    this.speed = speed;
  }

  public double getSpeed() {
    return speed;
  }

  /**
   * Estimates travel duration between two points using beeline distance.
   *
   * @param from Starting coordinate
   * @param to   Ending coordinate
   * @return Estimated duration
   */
  public Duration estimateDuration(WgsCoordinate from, WgsCoordinate to) {
    double beelineDistance = SphericalDistanceLibrary.fastDistance(
      from.asJtsCoordinate(),
      to.asJtsCoordinate()
    );
    double seconds = beelineDistance / speed;
    return Duration.ofSeconds((long) seconds);
  }

  /**
   * A lower bound on the time a car needs from one point to the other, given in degrees: the
   * beeline, driven at the fastest speed any street in the graph can be driven at.
   *
   * @param maxCarSpeed the graph's maximum car speed, in metres per second
   */
  public static double minDriveSeconds(
    double fromLat,
    double fromLon,
    double toLat,
    double toLon,
    double maxCarSpeed
  ) {
    return SphericalDistanceLibrary.fastDistance(fromLat, fromLon, toLat, toLon) / maxCarSpeed;
  }

  /** Like {@link #minDriveSeconds(double, double, double, double, double)}, for two coordinates. */
  public static double minDriveSeconds(Coordinate from, Coordinate to, double maxCarSpeed) {
    return SphericalDistanceLibrary.fastDistance(from, to) / maxCarSpeed;
  }
}
