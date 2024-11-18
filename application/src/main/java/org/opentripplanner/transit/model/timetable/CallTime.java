package org.opentripplanner.transit.model.timetable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * A scheduled time of a transit vehicle at a certain location with an optional realtime
 * information. This is meant to be used in timetables (not in transit legs).
 */
public class CallTime {

  private final ZonedDateTime scheduledTime;

  @Nullable
  private final CallRealTimeEstimate estimated;

  private CallTime(ZonedDateTime scheduledTime, @Nullable CallRealTimeEstimate estimated) {
    this.scheduledTime = Objects.requireNonNull(scheduledTime);
    this.estimated = estimated;
  }

  public static CallTime of(ZonedDateTime realtime, int delaySecs) {
    var delay = Duration.ofSeconds(delaySecs);
    return new CallTime(realtime.minus(delay), new CallRealTimeEstimate(realtime, delay));
  }

  public static CallTime ofStatic(ZonedDateTime staticTime) {
    return new CallTime(staticTime, null);
  }

  public ZonedDateTime scheduledTime() {
    return scheduledTime;
  }

  public CallRealTimeEstimate estimated() {
    return estimated;
  }
}
