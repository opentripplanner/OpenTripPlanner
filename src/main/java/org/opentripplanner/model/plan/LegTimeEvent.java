package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record LegTimeEvent(@Nonnull ZonedDateTime scheduled, @Nullable RealtimeEstimate estimated) {
  public LegTimeEvent {
    Objects.requireNonNull(scheduled);
  }
  @Nonnull
  public static LegTimeEvent of(ZonedDateTime realtime, int delaySecs) {
    var delay = Duration.ofSeconds(delaySecs);
    return new LegTimeEvent(realtime.minus(delay), new RealtimeEstimate(realtime, delay));
  }
  @Nonnull
  public static LegTimeEvent ofStatic(ZonedDateTime staticTime) {
    return new LegTimeEvent(staticTime, null);
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
