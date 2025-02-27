package org.opentripplanner.updater.trip.siri.mapping;

import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import uk.org.siri.siri20.OccupancyEnumeration;

/**
 * Maps the (very limited) SIRI 2.0 OccupancyEnum to internal OccupancyStatus
 */
public class OccupancyMapper {

  public static OccupancyStatus mapOccupancyStatus(OccupancyEnumeration occupancy) {
    if (occupancy == null) {
      return OccupancyStatus.NO_DATA_AVAILABLE;
    }
    return switch (occupancy) {
      case SEATS_AVAILABLE -> OccupancyStatus.MANY_SEATS_AVAILABLE;
      case STANDING_AVAILABLE -> OccupancyStatus.STANDING_ROOM_ONLY;
      case FULL -> OccupancyStatus.FULL;
    };
  }
}
