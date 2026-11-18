package org.opentripplanner.apis.gtfs.model;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * What is scheduled for a call. None of it is affected by real-time updates.
 *
 * @param time the scheduled time or time window of the call
 * @param pickupType the scheduled pickup type of the call
 * @param dropOffType the scheduled drop off type of the call
 * @param stopLocation the originally scheduled stop location of the call, which may differ from
 *                     the real-time stop location if the stop (e.g. platform) was changed by a
 *                     real-time update
 */
public record CallSchedule(
  CallScheduledTime time,
  @Nullable PickDrop pickupType,
  @Nullable PickDrop dropOffType,
  StopLocation stopLocation
) {}
