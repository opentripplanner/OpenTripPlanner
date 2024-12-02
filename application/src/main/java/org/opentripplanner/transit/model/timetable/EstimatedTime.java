package org.opentripplanner.transit.model.timetable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Realtime information about a vehicle at a certain place. This is meant to be used in timetables
 * (not in transit legs).
 */
public class EstimatedTime {

  private final ZonedDateTime time;
  private final Duration delay;

  /**
   * @param delay Delay or "earliness" of a vehicle. Earliness is expressed as a negative number.
   */
  private EstimatedTime(ZonedDateTime time, Duration delay) {
    this.time = Objects.requireNonNull(time);
    this.delay = Objects.requireNonNull(delay);
  }

  public static EstimatedTime of(ZonedDateTime scheduledTime, int delaySecs) {
    var delay = Duration.ofSeconds(delaySecs);
    return new EstimatedTime(scheduledTime.minus(delay), delay);
  }

  public ZonedDateTime time() {
    return time;
  }

  public Duration delay() {
    return delay;
  }
}
