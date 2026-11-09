package org.opentripplanner.apis.gtfs.model;

import org.opentripplanner.model.PickDrop;

public record CallSchedule(CallScheduledTime time, PickDrop pickupType, PickDrop dropOffType) {}
