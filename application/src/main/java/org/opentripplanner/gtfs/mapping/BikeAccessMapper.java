package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.transit.model.network.BikeAccess;

/**
 * Model bike access for GTFS trips by using the bikes_allowed fields from route and trip.
 */
class BikeAccessMapper {

  public static BikeAccess mapForTrip(Trip rhs) {
    return mapValues(rhs.getBikesAllowed());
  }

  public static BikeAccess mapForRoute(Route rhs) {
    return mapValues(rhs.getBikesAllowed());
  }

  private static BikeAccess mapValues(int bikesAllowed) {
    return switch (bikesAllowed) {
      case 1 -> BikeAccess.ALLOWED;
      case 2 -> BikeAccess.NOT_ALLOWED;
      default -> BikeAccess.UNKNOWN;
    };
  }
}
