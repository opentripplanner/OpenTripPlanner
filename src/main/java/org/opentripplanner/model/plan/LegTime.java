package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A scheduled time of a transit vehicle at a certain location with a optional realtime information.
 */
public record LegTime(@Nonnull ZonedDateTime scheduledTime, @Nullable RealTimeEstimate estimated) {
  public LegTime {
    Objects.requireNonNull(scheduledTime);
  }

  @Nonnull
  public static LegTime of(ZonedDateTime realtime, int delaySecs) {
    var delay = Duration.ofSeconds(delaySecs);
    return new LegTime(realtime.minus(delay), new RealTimeEstimate(realtime, delay));
  }

  @Nonnull
  public static LegTime ofStatic(ZonedDateTime staticTime) {
    return new LegTime(staticTime, null);
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
