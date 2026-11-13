package org.opentripplanner.apis.gtfs.model;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.model.PickDrop;

public record CallSchedule(
  CallScheduledTime time,
  @Nullable PickDrop pickupType,
  @Nullable PickDrop dropOffType
) {}
