package org.opentripplanner.apis.gtfs.model;

import org.opentripplanner.transit.model.timetable.OccupancyStatus;

/**
 * Record for holding trip occupancy information.
 */
public record TripOccupancy(OccupancyStatus occupancyStatus) {}
