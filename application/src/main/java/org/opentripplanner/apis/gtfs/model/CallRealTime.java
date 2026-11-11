package org.opentripplanner.apis.gtfs.model;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.transit.model.timetable.EstimatedTime;

/**
 * Real-time information of a call. More real-time state of the call may be added in the future.
 *
 * @param canceled whether a real-time update has canceled the call or its whole trip
 */
public record CallRealTime(
  @Nullable EstimatedTime arrival,
  @Nullable EstimatedTime departure,
  boolean canceled
) {}
