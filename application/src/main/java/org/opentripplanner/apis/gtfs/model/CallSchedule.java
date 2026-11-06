package org.opentripplanner.apis.gtfs.model;

/**
 * What is planned for a call.
 *
 * @param canceled whether the trip of the call is canceled or replaced in the planned data
 */
public record CallSchedule(CallScheduledTime time, boolean canceled) {}
