package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.geometry.WgsCoordinate;

public interface AccessEgressTripFilter {
  /** Runs filter to check whether the carpooling trip is potentially valid.
   *
   * @param trip Carpool trip
   * @param coordinateOfPassenger Coordinates of origin if access, and destination if egress
   * @param passengerDepartureTime Requested departure time of the passenger
   * @param searchWindow The time window around the requested departure time in which the trip will be considered
   * @return true if the filter passes, false if it doesn't
   */
  boolean acceptsAccessEgress(
    CarpoolTrip trip,
    WgsCoordinate coordinateOfPassenger,
    Instant passengerDepartureTime,
    Duration searchWindow
  );
}
