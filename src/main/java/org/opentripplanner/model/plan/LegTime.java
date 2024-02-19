package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A scheduled time of a transit vehicle at a certain location with a optional realtime information.
 */
public record LegTime(@Nonnull ZonedDateTime scheduled, @Nullable RealtimeEstimate estimated) {
  public LegTime {
    Objects.requireNonNull(scheduled);
  }
  @Nonnull
  public static LegTime of(ZonedDateTime realtime, int delaySecs) {
    var delay = Duration.ofSeconds(delaySecs);
    return new LegTime(realtime.minus(delay), new RealtimeEstimate(realtime, delay));
  }
  @Nonnull
  public static LegTime ofStatic(ZonedDateTime staticTime) {
    return new LegTime(staticTime, null);
  }

  public ZonedDateTime time() {
    if (estimated == null) {
      return scheduled;
    } else {
      return estimated.time;
    }
  }

  record RealtimeEstimate(ZonedDateTime time, Duration scheduleOffset) {}
}
