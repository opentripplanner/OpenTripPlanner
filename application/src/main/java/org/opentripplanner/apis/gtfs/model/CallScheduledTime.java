package org.opentripplanner.apis.gtfs.model;

import java.time.OffsetDateTime;
import javax.annotation.Nullable;

public sealed interface CallScheduledTime
  permits CallScheduledTime.TimeWindow, CallScheduledTime.ArrivalDepartureTime {
  record ArrivalDepartureTime(@Nullable OffsetDateTime arrival, @Nullable OffsetDateTime departure)
    implements CallScheduledTime {}

  record TimeWindow(OffsetDateTime start, OffsetDateTime end) implements CallScheduledTime {}
}
