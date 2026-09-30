package org.opentripplanner.core.model.basic;

import java.io.Serializable;
import org.opentripplanner.utils.lang.DoubleUtils;

public class Reluctance
  implements Serializable, Comparable<Reluctance>, NormalizedDistance<Reluctance>
{

  private static final double MIN_VALUE = 1.0;
  private static final double MAX_VALUE = 1_000_000.0;

  private static final double[][] SCALE = {
    { 2.0, 0.1 },
    { 5.0, 0.5 },
    { 10.0, 1.0 },
    { 20.0, 2.0 },
    { 50.0, 5.0 },
    { 100.0, 10.0 },
    { 200.0, 20.0 },
    { 500.0, 50.0 },
    { 1_000.0, 100.0 },
    { 2_000.0, 200.0 },
    { 5_000.0, 500.0 },
    { 10_000.0, 1_000.0 },
    { 100_000.0, 10_00.0 },
    { 1_000_000.0, 100_000.0 },
  };

  private static final LogaritmicScale LN_SCALE = new LogaritmicScale(MIN_VALUE, MAX_VALUE);

  private final double value;

  private Reluctance(double value) {
    DoubleUtils.requireInRange(value, MIN_VALUE, MAX_VALUE);
    this.value = DoubleUtils.roundToStep(value, step(value));
  }

  public static Reluctance of(double value) {
    return new Reluctance(value);
  }

  public double value() {
    return value;
  }

  @Override
  public String toString() {
    return value + "Ω";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    return o instanceof Reluctance s ? value == s.value : false;
  }

  @Override
  public int hashCode() {
    return Double.hashCode(value);
  }

  @Override
  public int compareTo(Reluctance o) {
    return Math.round(100.0f * (float) (value - o.value));
  }

  private static double step(double value) {
    for (double[] limit : SCALE) {
      if (value < limit[0]) {
        return limit[1];
      }
    }
    return MAX_VALUE / 10.0;
  }

  @Override
  public float normalizedDistance(Reluctance other) {
    return LN_SCALE.normalizedDistance(this.value, other.value);
  }
}
