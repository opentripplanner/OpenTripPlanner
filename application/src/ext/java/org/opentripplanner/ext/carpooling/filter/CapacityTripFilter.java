package org.opentripplanner.ext.carpooling.filter;

import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;

/**
 * Pre-filters carpool trips by whether the car has a free seat on at least one leg: a car that is
 * full all the way cannot take the passenger.
 */
public class CapacityTripFilter implements CarpoolTripFilter {

  @Override
  public boolean isCandidateTrip(RoutableCarpoolTrip trip, SnappedPassenger passenger) {
    var carpoolTrip = trip.trip();
    for (int leg = 0; leg < carpoolTrip.stops().size() - 1; leg++) {
      if (carpoolTrip.getPassengerCountAtDepartureOfStop(leg) < carpoolTrip.totalCapacity()) {
        return true;
      }
    }
    return false;
  }
}
