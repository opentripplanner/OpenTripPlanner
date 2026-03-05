package org.opentripplanner.framework.model;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.utils.time.DurationUtils;

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

  public TimeAndCost normalize() {
    var temp = new TimeAndCost(time.truncatedTo(ChronoUnit.SECONDS), cost.normalize());
    return equals(temp) ? this : temp;
  }

  @Override
  public String toString() {
    return "(" + DurationUtils.durationToStr(time) + " " + cost + ")";
  }
}
