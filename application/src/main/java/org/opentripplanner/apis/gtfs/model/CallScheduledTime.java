package org.opentripplanner.apis.gtfs.model;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import javax.annotation.Nullable;

public sealed interface CallScheduledTime
  permits CallScheduledTime.TimeWindow, CallScheduledTime.ArrivalDepartureTime
{
  record ArrivalDepartureTime(
    @Nullable OffsetDateTime arrival,
    @Nullable OffsetDateTime departure,
    boolean timePoint
  ) implements CallScheduledTime {
    public ArrivalDepartureTime(
      @Nullable ZonedDateTime arrival,
      @Nullable ZonedDateTime departure,
      boolean timePoint
    ) {
      this(
        arrival == null ? null : arrival.toOffsetDateTime(),
        departure == null ? null : departure.toOffsetDateTime(),
        timePoint
      );
    }
  }

  record TimeWindow(OffsetDateTime start, OffsetDateTime end) implements CallScheduledTime {}
}
