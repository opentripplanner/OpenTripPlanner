package org.opentripplanner.transit.model.timetable;

import org.opentripplanner.framework.doc.DocumentedEnum;

/**
 * OccupancyStatus to be exposed in the API. The values are based on GTFS-RT
 * (transit_realtime.VehiclePosition.OccupancyStatus) that can be easily be mapped to the Nordic
 * SIRI-profile (SIRI 2.1)
 * <p>
 * Descriptions are copied from the GTFS-RT specification with additions of SIRI nordic profile documentation.
 */
public enum OccupancyStatus implements DocumentedEnum<OccupancyStatus> {
  NO_DATA_AVAILABLE,
  EMPTY,
  MANY_SEATS_AVAILABLE,
  FEW_SEATS_AVAILABLE,
  STANDING_ROOM_ONLY,
  CRUSHED_STANDING_ROOM_ONLY,
  FULL,
  NOT_ACCEPTING_PASSENGERS;

  @Override
  public String typeDescription() {
    return "OccupancyStatus to be exposed in the API. The values are based on GTFS-RT";
  }

  @Override
  public String enumValueDescription() {
    return switch (this) {
      case NO_DATA_AVAILABLE -> "The vehicle or carriage doesn't have any occupancy data available.";
      case EMPTY -> """
      The vehicle is considered empty by most measures, and has few or no passengers onboard, but is
      still accepting passengers. There isn't a big difference between this and `manySeatsAvailable`
      so it's possible to handle them as the same value, if one wants to limit the number of different
      values.
      SIRI nordic profile: merge these into `manySeatsAvailable`.
      """;
      case MANY_SEATS_AVAILABLE -> """
      The vehicle or carriage has a large number of seats available.
      SIRI nordic profile: more than ~50% of seats available.
      """;
      case FEW_SEATS_AVAILABLE -> """
      The vehicle or carriage has a few seats available.
      SIRI nordic profile: less than ~50% of seats available.
      """;
      case STANDING_ROOM_ONLY -> """
      The vehicle or carriage only has standing room available.
      SIRI nordic profile: less than ~10% of seats available.
      """;
      case CRUSHED_STANDING_ROOM_ONLY -> """
      The vehicle or carriage can currently accommodate only standing passengers and has limited
      space for them. There isn't a big difference between this and `full` so it's possible to
      handle them as the same value, if one wants to limit the number of different values.
      SIRI nordic profile: merge into `standingRoomOnly`.
      """;
      case FULL -> """
      The vehicle or carriage is considered full by most measures, but may still be allowing
      passengers to board.
      """;
      case NOT_ACCEPTING_PASSENGERS -> """
      The vehicle or carriage has no seats or standing room available.
      SIRI nordic profile: if vehicle/carriage is not in use / unavailable, or passengers are only
      allowed to alight due to e.g. crowding.
      """;
    };
  }
}
