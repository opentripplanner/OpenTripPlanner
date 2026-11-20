package org.opentripplanner.apis.gtfs.model;

import org.opentripplanner.transit.model.site.StopLocation;

/**
 * What is scheduled for a call: the scheduled time and the originally scheduled stop location.
 * Neither is affected by real-time updates.
 *
 * @param time the scheduled time or time window of the call
 * @param stopLocation the originally scheduled stop location of the call, which may differ from
 *                     the real-time stop location if the stop (e.g. platform) was changed by a
 *                     real-time update
 */
public record CallSchedule(CallScheduledTime time, StopLocation stopLocation) {}
