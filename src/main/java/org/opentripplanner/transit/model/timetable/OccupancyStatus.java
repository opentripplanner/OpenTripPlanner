package org.opentripplanner.transit.model.timetable;

/**
 * OccupancyStatus to be exposed in the API. The values are based on GTFS-RT
 * (transit_realtime.VehiclePosition.OccupancyStatus), but is currently a subset that easily can
 * be mapped to the Nordic SIRI-profile (SIRI 2.1)
 *
 * Descriptions are also based on the SIRI-profile
 */
public enum OccupancyStatus {
  /**
   * Default. There is no occupancy-data on this departure
   */
  NO_DATA,
  /**
   * More than ~50% of seats available
   */
  MANY_SEATS_AVAILABLE,
  /**
   * Less than ~50% of seats available
   */
  SEATS_AVAILABLE,
  /**
   * Less than ~10% of seats available
   */
  STANDING_ROOM_ONLY,
  /**
   * Close to or at full capacity
   */
  FULL,
  /**
   * If vehicle/carriage is not in use / unavailable, or passengers are only allowed to alight due to e.g. crowding
   */
  NOT_ACCEPTING_PASSENGERS,
}
