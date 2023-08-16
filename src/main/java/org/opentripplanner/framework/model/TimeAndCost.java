package org.opentripplanner.framework.model;

import java.time.Duration;
import org.opentripplanner.framework.time.DurationUtils;

/**
 * Tuple of time(duration) and cost.
 */
public record TimeAndCost(Duration time, Cost cost) {
  public static final TimeAndCost ZERO = new TimeAndCost(Duration.ZERO, Cost.ZERO);

  public int timeInSeconds() {
    return (int) time.toSeconds();
  }

  public boolean isZero() {
    return this.equals(ZERO);
  }

  @Override
  public String toString() {
    return "(" + DurationUtils.durationToStr(time) + " " + cost + ")";
  }
}
