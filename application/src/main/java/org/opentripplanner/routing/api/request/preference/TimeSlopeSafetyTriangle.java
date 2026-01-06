package org.opentripplanner.routing.api.request.preference;

import static org.opentripplanner.utils.lang.DoubleUtils.doubleEquals;

/**
 * Sets the (bicycle or scooter) triangle routing parameters -- the relative importance of safety,
 * flatness, and speed. These three fields _should_ have values between 0 and 1, and _should_ add up to
 * 1, these constraints are not enforced in this class, however, and any value is accepted. Instead,
 * the values are normalized in the domain layer.
 * <p>
 * This class is currently only used with bicycle routing, but is not limited to that.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public record TimeSlopeSafetyTriangle(double time, double slope, double safety) {
  private static final double ZERO = 0.0;

  public static final TimeSlopeSafetyTriangle DEFAULT = new TimeSlopeSafetyTriangle(1, 1, 1);

  public TimeSlopeSafetyTriangle.Builder copyOf() {
    return new Builder(this);
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

  private static boolean zeroVector(double a, double b, double c) {
    return a == ZERO && b == ZERO && c == ZERO;
  }

  public static class Builder {

    private double time;
    private double slope;
    private double safety;

    private Builder(TimeSlopeSafetyTriangle original) {
      this.time = original.time;
      this.slope = original.slope;
      this.safety = original.safety;
    }

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
