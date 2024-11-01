package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * A scheduled time of a transit vehicle at a certain location with an optional realtime
 * information.
 */
public record FixedArrivalDepartureTime(
  ZonedDateTime scheduledTime,
  @Nullable RealTimeEstimate estimated
) {
  public FixedArrivalDepartureTime {
    Objects.requireNonNull(scheduledTime);
  }

  public static FixedArrivalDepartureTime of(ZonedDateTime realtime, int delaySecs) {
    var delay = Duration.ofSeconds(delaySecs);
    return new FixedArrivalDepartureTime(
      realtime.minus(delay),
      new RealTimeEstimate(realtime, delay)
    );
  }

  public static FixedArrivalDepartureTime ofStatic(ZonedDateTime staticTime) {
    return new FixedArrivalDepartureTime(staticTime, null);
  }

  /**
   * The most up-to-date time available: if realtime data is available it is returned, if not then
   * the scheduled one is.
   */
  public ZonedDateTime time() {
    if (estimated == null) {
      return scheduledTime;
    } else {
      return estimated.time;
    }
  }

  /**
   * Realtime information about a vehicle at a certain place.
   * @param delay Delay or "earliness" of a vehicle. Earliness is expressed as a negative number.
   */
  record RealTimeEstimate(ZonedDateTime time, Duration delay) {}
}
