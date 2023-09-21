package org.opentripplanner.ext.gtfsgraphqlapi.model;

import org.opentripplanner.transit.model.timetable.OccupancyStatus;

/**
 * Record for holding trip occupancy information.
 */
public record TripOccupancy(OccupancyStatus occupancyStatus) {}
