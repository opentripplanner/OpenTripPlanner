package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.transit.model.network.CarAccess;

/**
 * Model car access for GTFS trips.
 */
class CarAccessMapper {

  public static CarAccess mapForTrip(Trip rhs) {
    int carsAllowed = rhs.getCarsAllowed();
    switch (carsAllowed) {
      case 1:
        return CarAccess.ALLOWED;
      case 2:
        return CarAccess.NOT_ALLOWED;
      default:
        return CarAccess.UNKNOWN;
    }
  }
}
