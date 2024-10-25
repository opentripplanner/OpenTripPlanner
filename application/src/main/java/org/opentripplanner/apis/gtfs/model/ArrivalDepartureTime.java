package org.opentripplanner.apis.gtfs.model;

import java.time.Duration;
import java.time.ZonedDateTime;
import javax.annotation.Nullable;

/**
 * Timing of an arrival or a departure to or from a stop. May contain real-time information
 * if available.
 */
public record ArrivalDepartureTime(
  ZonedDateTime scheduledTime,
  @Nullable RealTimeEstimate estimated
) {
  public static ArrivalDepartureTime of(ZonedDateTime realtime, int delaySecs) {
    var delay = Duration.ofSeconds(delaySecs);
    return new ArrivalDepartureTime(realtime.minus(delay), new RealTimeEstimate(realtime, delay));
  }

  public static ArrivalDepartureTime ofStatic(ZonedDateTime staticTime) {
    return new ArrivalDepartureTime(staticTime, null);
  }

  /**
   * Realtime information about a vehicle at a certain place.
   * @param delay Delay or "earliness" of a vehicle. Earliness is expressed as a negative number.
   */
  record RealTimeEstimate(ZonedDateTime time, Duration delay) {}
}
