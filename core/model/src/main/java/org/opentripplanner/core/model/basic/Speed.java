package org.opentripplanner.core.model.basic;

import java.io.Serializable;
import org.opentripplanner.utils.lang.DoubleUtils;

/**
 * Ground speed - in meters per second, limited to 300 m/s.
 */
public class Speed implements Serializable, Comparable<Speed>, NormalizedDistance<Speed> {

  private static final double MIN_VALUE = 0.1;
  private static final double MAX_VALUE = 300.0;
  private static final LogaritmicScale LN_SCALE = new LogaritmicScale(MIN_VALUE, MAX_VALUE);
  /**
   * The step the values are rounded to, as rows of {@code {upper limit, step}}, see
   * {@link DoubleUtils#roundToStep(double, double[][])}.
   */
  private static final double[][] SCALE = {
    { 2.0, 0.05 },
    { 5.0, 0.1 },
    { 10.0, 0.2 },
    { 20.0, 0.5 },
    { 50.0, 1.0 },
    { 100.0, 2.0 },
    { Double.MAX_VALUE, 5.0 },
  };

  private final double value;

  private Speed(double value) {
    DoubleUtils.requireInRange(value, MIN_VALUE, MAX_VALUE);
    this.value = DoubleUtils.roundToStep(value, SCALE);
  }

  public static Speed ofMetersPerSecond(double value) {
    return new Speed(value);
  }

  public double toMetersPerSecond() {
    return value;
  }

  @Override
  public String toString() {
    return value + "m/s";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    return o instanceof Speed s ? value == s.value : false;
  }

  @Override
  public int hashCode() {
    return Double.hashCode(value);
  }

  @Override
  public int compareTo(Speed o) {
    return Math.round(100.0f * (float) (value - o.value));
  }

  @Override
  public float normalizedDistance(Speed other) {
    return LN_SCALE.normalizedDistance(this.value, other.value);
  }
}
