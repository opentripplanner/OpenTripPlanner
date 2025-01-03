package org.opentripplanner.apis.gtfs.model;

import java.time.OffsetDateTime;
import javax.annotation.Nullable;

public record ArrivalDepartureTime(
  @Nullable OffsetDateTime arrival,
  @Nullable OffsetDateTime departure
) {}
