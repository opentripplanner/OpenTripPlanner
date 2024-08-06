package org.opentripplanner.routing.api.request.preference;

import static org.opentripplanner.framework.lang.DoubleUtils.doubleEquals;
import static org.opentripplanner.framework.lang.DoubleUtils.roundTo2Decimals;

/**
 * Sets the (bicycle or scooter) triangle routing parameters -- the relative importance of safety,
 * flatness, and speed. These three fields should have values between 0 and 1, and should add up to
 * 1.
 * <p>
 * The constructor accepts any three numbers and will normalize them to add up to 1. {@code time}
 * and {@code slope} are rounded to the closest two decimal number, then
 * {@code safety := 1.0 - (time + slope)}. This is done to make the rounding predictable and to
 * allways add up to one. This allows this class to be used in an index of a cache. For example:
 * <pre>
 *   ( 1.0, 1.0, 1.0 ) => ( time: 0.33, slope: 0.33, safety: 0.34 )
 * </pre>
 * <p>
 * This class is currently only used with bicycle routing, but is not limited to that.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public record TimeSlopeSafetyTriangle(double time, double slope, double safety) {
  private static final double ZERO = 0.0;

  public static final TimeSlopeSafetyTriangle DEFAULT = new TimeSlopeSafetyTriangle(1, 1, 1);

  /**
   * Sets the bicycle or scooter triangle routing parameters -- the relative importance of safety,
   * flatness, and speed. These three fields of the RouteRequest should have values between 0 and 1,
   * and should add up to 1. This setter function accepts any three numbers and will normalize them
   * to add up to 1.
   */
  public TimeSlopeSafetyTriangle(double time, double slope, double safety) {
    safety = positiveValueOrZero(safety);
    slope = positiveValueOrZero(slope);
    time = positiveValueOrZero(time);

    if (zeroVector(time, slope, safety)) {
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

  /**
   * Creates a special builder, which used together with the
   * {@link Builder#buildOrDefault(TimeSlopeSafetyTriangle)} can return a new instance or the
   * default value if no values are set. This is useful in the APIs where we want to fall back to
   * the default {@link TimeSlopeSafetyTriangle}, if no values are set. If only one or two values
   * are set the new instance will contain only those values, and the none set values will be zero.
   */
  public static TimeSlopeSafetyTriangle.Builder of() {
    return new Builder();
  }

  private static double positiveValueOrZero(double value) {
    return Math.max(0, value);
  }

  private static boolean zeroVector(double a, double b, double c) {
    return a == ZERO && b == ZERO && c == ZERO;
  }

  public static class Builder {

    private double time;
    private double slope;
    private double safety;

    private Builder() {
      this.time = ZERO;
      this.slope = ZERO;
      this.safety = ZERO;
    }

    public double time() {
      return time;
    }

    public Builder withTime(double time) {
      this.time = time;
      return this;
    }

    public double slope() {
      return slope;
    }

    public Builder withSlope(double slope) {
      this.slope = slope;
      return this;
    }

    public double safety() {
      return safety;
    }

    public Builder withSafety(double safety) {
      this.safety = safety;
      return this;
    }

    /**
     * Returns true if none of the values are set (i.e. all values are zero).
     */
    public boolean isEmpty() {
      return doubleEquals(time, ZERO) && doubleEquals(slope, ZERO) && doubleEquals(safety, ZERO);
    }

    public TimeSlopeSafetyTriangle build() {
      return new TimeSlopeSafetyTriangle(time, slope, safety);
    }

    public TimeSlopeSafetyTriangle buildOrDefault(TimeSlopeSafetyTriangle defaultValue) {
      // If none of the fields are set, fallback to the default values given
      if (zeroVector(time, slope, safety)) {
        return defaultValue;
      }
      return new TimeSlopeSafetyTriangle(time, slope, safety);
    }
  }
}
