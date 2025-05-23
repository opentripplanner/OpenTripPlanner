package org.opentripplanner.model.plan.leg;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * A scheduled time of a transit vehicle at a certain location with an optional realtime
 * information. This is meant to be used in transit legs.
 */
public class LegCallTime {

  private final ZonedDateTime scheduledTime;

  @Nullable
  private final LegRealTimeEstimate estimated;

  private LegCallTime(ZonedDateTime scheduledTime, @Nullable LegRealTimeEstimate estimated) {
    this.scheduledTime = Objects.requireNonNull(scheduledTime);
    this.estimated = estimated;
  }

  public static LegCallTime of(ZonedDateTime realtime, int delaySecs) {
    var delay = Duration.ofSeconds(delaySecs);
    return new LegCallTime(realtime.minus(delay), new LegRealTimeEstimate(realtime, delay));
  }

  public static LegCallTime ofStatic(ZonedDateTime staticTime) {
    return new LegCallTime(staticTime, null);
  }

  public ZonedDateTime scheduledTime() {
    return scheduledTime;
  }

  public LegRealTimeEstimate estimated() {
    return estimated;
  }

  /**
   * The most up-to-date time available: if realtime data is available it is returned, if not then
   * the scheduled one is.
   */
  public ZonedDateTime time() {
    if (estimated == null) {
      return scheduledTime;
    } else {
      return estimated.time();
    }
  }
}
