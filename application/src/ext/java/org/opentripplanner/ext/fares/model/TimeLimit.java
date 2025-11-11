package org.opentripplanner.ext.fares.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * Represents a time limit with a specific type and duration. This is often used to calculate
 * validity durations across a set of legs for fare calculation or similar purposes.
 * <p>
 * Instances of this class are immutable.
 */
public class TimeLimit implements Serializable {

  private final TimeLimitType type;
  private final Duration duration;

  public TimeLimit(TimeLimitType type, Duration duration) {
    this.type = Objects.requireNonNull(type);
    this.duration = DurationUtils.requireNonNegative(duration);
  }

  public TimeLimitType type() {
    return type;
  }

  public Duration duration() {
    return duration;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    TimeLimit timeLimit = (TimeLimit) o;
    return type == timeLimit.type && Objects.equals(duration, timeLimit.duration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, duration);
  }
}
