package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record LegTimes(
  @Nonnull ZonedDateTime scheduled,
  @Nullable ZonedDateTime actual,
  @Nullable Duration delay
) {
  public LegTimes {
    Objects.requireNonNull(scheduled);
  }
  @Nonnull
  public static LegTimes of(ZonedDateTime realtime, int delaySecs) {
    var delay = Duration.ofSeconds(delaySecs);
    return new LegTimes(realtime.minus(delay), realtime, delay);
  }
  @Nonnull
  public static LegTimes ofStatic(ZonedDateTime staticTime) {
    return new LegTimes(staticTime, staticTime, Duration.ZERO);
  }
}
