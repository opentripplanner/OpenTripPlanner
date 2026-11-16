package org.opentripplanner.apis.gtfs.model;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.transit.model.timetable.EstimatedTime;

/**
 * Real-time information and state of a call.
 *
 * @param canceled whether a real-time update has canceled the call
 */
public record CallRealTime(
  @Nullable EstimatedTime arrival,
  @Nullable EstimatedTime departure,
  boolean canceled
) {}
