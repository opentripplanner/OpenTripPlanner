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

  private final double value;

  private Speed(double value) {
    DoubleUtils.requireInRange(value, MIN_VALUE, MAX_VALUE);
    this.value = DoubleUtils.roundToStep(value, step(value));
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

  private static double step(double value) {
    if (value < 2.0) {
      return 0.05;
    }
    if (value < 30.0) {
      return 0.1;
    }
    return 1;
  }

  @Override
  public float normalizedDistance(Speed other) {
    return LN_SCALE.normalizedDistance(this.value, other.value);
  }
}
