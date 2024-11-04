package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.transit.model.network.CarAccess;

/**
 * Model car access for GTFS trips.
 */
class CarAccessMapper {

  public static CarAccess mapForTrip(Trip rhs) {
    int carsAllowed = rhs.getCarsAllowed();
    return switch (carsAllowed) {
      case 1 -> CarAccess.ALLOWED;
      case 2 -> CarAccess.NOT_ALLOWED;
      default -> CarAccess.UNKNOWN;
    };
  }
}
