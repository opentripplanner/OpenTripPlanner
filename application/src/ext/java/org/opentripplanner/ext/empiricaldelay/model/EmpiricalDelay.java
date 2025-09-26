package org.opentripplanner.ext.empiricaldelay.model;

import java.io.Serializable;
import java.time.Duration;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * Empirical dalay values in seconds.
 */
public class EmpiricalDelay implements Serializable {

  private final int p50;
  private final int p90;

  public EmpiricalDelay(int p50, int p90) {
    this.p50 = p50;
    this.p90 = p90;
  }

  public Duration p50() {
    return Duration.ofSeconds(p50);
  }

  public Duration p90() {
    return Duration.ofSeconds(p90);
  }

  @Override
  public String toString() {
    return "[" + DurationUtils.durationToStr(p50) + ", " + DurationUtils.durationToStr(p90) + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmpiricalDelay that = (EmpiricalDelay) o;
    return p50 == that.p50 && p90 == that.p90;
  }

  @Override
  public int hashCode() {
    return p50 + 31 * p90;
  }
}
