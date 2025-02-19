package org.opentripplanner.ext.vdv;

import java.time.Duration;
import java.util.stream.Stream;
import org.opentripplanner.model.TripTimeOnDate;

public record CallAtStop(TripTimeOnDate tripTimeOnDate, Duration walkTime) {
  public static CallAtStop noWalking(TripTimeOnDate tt) {
    return new CallAtStop(tt, null);
  }

  public CallAtStop withWalkTime(Duration duration) {
    return new CallAtStop(tripTimeOnDate, duration);
  }
}
