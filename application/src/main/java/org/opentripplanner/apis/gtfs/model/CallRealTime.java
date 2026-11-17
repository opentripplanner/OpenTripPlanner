package org.opentripplanner.apis.gtfs.model;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.transit.model.timetable.EstimatedTime;

public record CallRealTime(@Nullable EstimatedTime arrival, @Nullable EstimatedTime departure) {}
