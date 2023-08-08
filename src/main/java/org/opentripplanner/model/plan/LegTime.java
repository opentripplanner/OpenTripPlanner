package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.ZonedDateTime;
import javax.annotation.Nonnull;

public record LegTime(ZonedDateTime scheduled, ZonedDateTime actual, Duration delay) {
  @Nonnull
  public static LegTime of(ZonedDateTime realtime, int delaySecs) {
    var delay = Duration.ofSeconds(delaySecs);
    return new LegTime(realtime.minus(delay), realtime, delay);
  }
  @Nonnull
  public static LegTime ofStatic(ZonedDateTime staticTime) {
    return new LegTime(staticTime, staticTime, Duration.ZERO);
  }
}
