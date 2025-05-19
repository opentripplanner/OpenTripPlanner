package org.opentripplanner.model.plan.leg;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Realtime information about a vehicle at a certain place. Meant to be used in transit legs.
 */
public class LegRealTimeEstimate {

  private final ZonedDateTime time;
  private final Duration delay;

  /**
   * @param delay Delay or "earliness" of a vehicle. Earliness is expressed as a negative number.
   */
  public LegRealTimeEstimate(ZonedDateTime time, Duration delay) {
    this.time = Objects.requireNonNull(time);
    this.delay = Objects.requireNonNull(delay);
  }

  public ZonedDateTime time() {
    return time;
  }

  public Duration delay() {
    return delay;
  }
}
