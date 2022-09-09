package org.opentripplanner.routing.api.request.preference;

import static org.opentripplanner.util.lang.DoubleUtils.roundTo2Decimals;

/**
 * Sets the (bicycle) triangle routing parameters -- the relative importance of safety, flatness,
 * and speed. These three fields should have values between 0 and 1, and should add up to 1.
 * <p>
 * The constructor accepts any three numbers and will normalize them to add up to 1. {@code time}
 * and {@code slope} are rounded to the closest two decimal number, then
 * {@code safety := 1.0 - (time + slope)}. This is done to make the rounding predictable and
 * to allways add up to one. This allows this class to be used in an index of a cache. For example:
 * <pre>
 *   ( 1.0, 1.0, 1.0 ) => ( time: 0.33, slope: 0.33, safety: 0.34 )
 * </pre>
 * <p>
 * This class is currently only used with bicycle routing, but is not limited to that.
 */
public record TimeSlopeSafetyTriangle(double time, double slope, double safety) {
  public static final TimeSlopeSafetyTriangle DEFAULT = new TimeSlopeSafetyTriangle(1, 1, 1);

  /**
   * Sets the bicycle triangle routing parameters -- the relative importance of safety, flatness,
   * and speed. These three fields of the RoutingRequest should have values between 0 and 1, and
   * should add up to 1. This setter function accepts any three numbers and will normalize them to
   * add up to 1.
   */
  public TimeSlopeSafetyTriangle(double time, double slope, double safety) {
    safety = positiveValueOrZero(safety);
    slope = positiveValueOrZero(slope);
    time = positiveValueOrZero(time);

    if (safety == 0 && slope == 0 && time == 0) {
      time = 1.0;
      slope = 1.0;
      safety = 1.0;
    }

    // Normalize
    double total = safety + slope + time;
    time /= total;
    slope /= total;

    // We round to closest 2 decimals
    this.time = roundTo2Decimals(time);
    this.slope = roundTo2Decimals(slope);
    this.safety = roundTo2Decimals(1.0 - (this.time + this.slope));
  }

  private static double positiveValueOrZero(double value) {
    return Math.max(0, value);
  }
}
