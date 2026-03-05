package org.opentripplanner.ext.carpooling.util;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Provides fast, low-resolution travel time estimates based on beeline (straight-line) distances.
 * <p>
 * Used as a heuristic to quickly reject incompatible insertion positions before
 * performing expensive A* street routing. The estimates are intentionally optimistic
 * (lower bounds) to ensure we never incorrectly reject valid insertions.
 * <p>
 * Formula: duration = (beeline_distance × detour_factor) / average_speed (in m/s)
 * <p>
 * The detour factor accounts for the fact that street routes are rarely straight lines.
 * Typical values: 1.2-1.5, with 1.3 being a reasonable default for urban areas.
 */
public class BeelineEstimator {

  /**
   * Default detour factor: 1.3
   * Assumes actual street routes are ~30% longer than straight-line distance.
   * This is conservative - works well for most urban areas.
   */
  public static final double DEFAULT_DETOUR_FACTOR = 1.3;

  /**
   * Default average speed: 10 m/s (~36 km/h or ~22 mph)
   * Typical urban carpooling speed accounting for traffic, turns, stops.
   */
  public static final double DEFAULT_SPEED_MPS = 10.0;

  private final double detourFactor;
  private final double speed;

  /**
   * Creates estimator with default parameters.
   */
  public BeelineEstimator() {
    this(DEFAULT_DETOUR_FACTOR, DEFAULT_SPEED_MPS);
  }

  /**
   * Creates estimator with custom parameters.
   *
   * @param detourFactor Factor by which street routes are longer than beeline (typically 1.2-1.5)
   * @param speed Average travel speed in meters per second
   */
  public BeelineEstimator(double detourFactor, double speed) {
    if (detourFactor < 1.0) {
      throw new IllegalArgumentException("detourFactor must be >= 1.0 (got " + detourFactor + ")");
    }
    if (speed <= 0) {
      throw new IllegalArgumentException("speedMps must be positive (got " + speed + ")");
    }
    this.detourFactor = detourFactor;
    this.speed = speed;
  }

  public double getDetourFactor() {
    return detourFactor;
  }

  public double getSpeed() {
    return speed;
  }

  /**
   * Estimates travel duration between two points using beeline distance.
   *
   * @param from Starting coordinate
   * @param to Ending coordinate
   * @return Estimated duration
   */
  public Duration estimateDuration(WgsCoordinate from, WgsCoordinate to) {
    double beelineDistance = SphericalDistanceLibrary.fastDistance(
      from.asJtsCoordinate(),
      to.asJtsCoordinate()
    );
    double routeDistance = beelineDistance * detourFactor;
    double seconds = routeDistance / speed;
    return Duration.ofSeconds((long) seconds);
  }

  /**
   * Calculates cumulative travel times to each point in a route.
   * Returns an array where index i contains the cumulative duration from the start to point i.
   *
   * @param points Route points in order
   * @return Array of cumulative durations (first element is always Duration.ZERO)
   */
  public Duration[] calculateCumulativeTimes(List<WgsCoordinate> points) {
    if (points.isEmpty()) {
      return new Duration[0];
    }

    Duration[] cumulativeTimes = new Duration[points.size()];
    cumulativeTimes[0] = Duration.ZERO;

    for (int i = 0; i < points.size() - 1; i++) {
      Duration segmentDuration = estimateDuration(points.get(i), points.get(i + 1));
      cumulativeTimes[i + 1] = cumulativeTimes[i].plus(segmentDuration);
    }

    return cumulativeTimes;
  }
}
