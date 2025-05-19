package org.opentripplanner.updater.trip.siri.mapping;

import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import uk.org.siri.siri21.OccupancyEnumeration;

/**
 * Maps SIRI 2.1 OccupancyEnum to internal OccupancyStatus
 */
public class OccupancyMapper {

  public static OccupancyStatus mapOccupancyStatus(OccupancyEnumeration occupancy) {
    if (occupancy == null) {
      return OccupancyStatus.NO_DATA_AVAILABLE;
    }
    return switch (occupancy) {
      case EMPTY -> OccupancyStatus.EMPTY;
      case SEATS_AVAILABLE, MANY_SEATS_AVAILABLE -> OccupancyStatus.MANY_SEATS_AVAILABLE;
      case FEW_SEATS_AVAILABLE -> OccupancyStatus.FEW_SEATS_AVAILABLE;
      case STANDING_AVAILABLE -> OccupancyStatus.STANDING_ROOM_ONLY;
      case STANDING_ROOM_ONLY -> OccupancyStatus.STANDING_ROOM_ONLY;
      case CRUSHED_STANDING_ROOM_ONLY -> OccupancyStatus.CRUSHED_STANDING_ROOM_ONLY;
      case FULL -> OccupancyStatus.FULL;
      case NOT_ACCEPTING_PASSENGERS -> OccupancyStatus.NOT_ACCEPTING_PASSENGERS;
      case UNDEFINED, UNKNOWN -> OccupancyStatus.NO_DATA_AVAILABLE;
    };
  }
}
