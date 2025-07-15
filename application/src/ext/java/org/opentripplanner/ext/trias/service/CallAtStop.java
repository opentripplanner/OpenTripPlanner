package org.opentripplanner.ext.trias.service;

import java.time.Duration;
import java.util.Comparator;
import javax.annotation.Nullable;
import org.opentripplanner.model.TripTimeOnDate;

public record CallAtStop(TripTimeOnDate tripTimeOnDate, @Nullable Duration walkTime) {
  public static CallAtStop noWalking(TripTimeOnDate tt) {
    return new CallAtStop(tt, null);
  }

  public CallAtStop withWalkTime(Duration duration) {
    return new CallAtStop(tripTimeOnDate, duration);
  }

  public static Comparator<CallAtStop> compareByScheduledDeparture() {
    return Comparator.comparing(
      tt ->
        tt.tripTimeOnDate().getServiceDayMidnight() + tt.tripTimeOnDate().getScheduledDeparture()
    );
  }
}
