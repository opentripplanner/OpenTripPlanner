package org.opentripplanner.ext.vdv;

import java.time.Duration;
import org.opentripplanner.model.TripTimeOnDate;

public record CallAtStop(TripTimeOnDate tripTimeOnDate, Duration walkTime) {
  public static CallAtStop noWalking(TripTimeOnDate tt) {
    return new CallAtStop(tt, null);
  }
}
