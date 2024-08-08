package org.opentripplanner.apis.gtfs.model;

import java.time.Duration;
import java.time.ZonedDateTime;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A scheduled time of a trip's start or end with an optional realtime information.
 */
public record DatedTripTime(
  @Nonnull ZonedDateTime scheduledTime,
  @Nullable RealTimeEstimate estimated
) {
  @Nonnull
  public static DatedTripTime of(ZonedDateTime realtime, int delaySecs) {
    var delay = Duration.ofSeconds(delaySecs);
    return new DatedTripTime(realtime.minus(delay), new RealTimeEstimate(realtime, delay));
  }

  @Nonnull
  public static DatedTripTime ofStatic(ZonedDateTime staticTime) {
    return new DatedTripTime(staticTime, null);
  }

  /**
   * Realtime information about a vehicle at a certain place.
   * @param delay Delay or "earliness" of a vehicle. Earliness is expressed as a negative number.
   */
  record RealTimeEstimate(ZonedDateTime time, Duration delay) {}
}
