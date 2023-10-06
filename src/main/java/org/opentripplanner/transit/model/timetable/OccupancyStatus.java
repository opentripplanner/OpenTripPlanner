package org.opentripplanner.transit.model.timetable;

/**
 * OccupancyStatus to be exposed in the API. The values are based on GTFS-RT
 * (transit_realtime.VehiclePosition.OccupancyStatus) that can be easily be mapped to the Nordic
 * SIRI-profile (SIRI 2.1)
 * <p>
 * Descriptions are copied from the GTFS-RT specification.
 */
public enum OccupancyStatus {
  /**
   * Default. There is no occupancy-data on this departure,
   */
  NO_DATA_AVAILABLE,
  /**
   * The vehicle is considered empty by most measures, and has few or no passengers onboard, but is
   * still accepting passengers.
   */
  EMPTY,
  /**
   * The vehicle or carriage has a large number of seats available. The amount of free seats out of
   * the total seats available to be considered large enough to fall into this category is
   * determined at the discretion of the producer.
   */
  MANY_SEATS_AVAILABLE,
  /**
   * The vehicle or carriage has a small number of seats available. The amount of free seats out of
   * the total seats available to be considered small enough to fall into this category is
   * determined at the discretion of the producer.
   * SIRI nordic profile: more than ~50% of seats available.
   */
  FEW_SEATS_AVAILABLE,
  /**
   * The vehicle or carriage can currently accommodate only standing passengers.
   * SIRI nordic profile: less than ~50% of seats available.
   */
  STANDING_ROOM_ONLY,
  /**
   * The vehicle or carriage can currently accommodate only standing passengers and has limited
   * space for them.
   * SIRI nordic profile: less than ~10% of seats available.
   */
  CRUSHED_STANDING_ROOM_ONLY,
  /**
   * The vehicle is considered full by most measures, but may still be allowing passengers to
   * board.
   */
  FULL,
  /**
   * The vehicle or carriage is not accepting passengers.
   * SIRI nordic profile: if vehicle/carriage is not in use / unavailable, or passengers are only
   * allowed to alight due to e.g. crowding.
   */
  NOT_ACCEPTING_PASSENGERS,
}
