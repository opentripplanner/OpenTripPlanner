package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters trips based on available capacity.
 * <p>
 * This is a fast pre-filter that checks if the trip has any capacity at all.
 * More detailed per-position capacity checking happens during insertion validation.
 */
public class CapacityFilter implements TripFilter, AccessEgressTripFilter {

  private static final Logger LOG = LoggerFactory.getLogger(CapacityFilter.class);

  private boolean accepts(CarpoolTrip trip) {
    boolean hasCapacity = trip.availableSeats() > 0;

    if (!hasCapacity) {
      LOG.debug("Trip {} rejected by capacity filter: no available seats", trip.getId());
    }

    return hasCapacity;
  }

  @Override
  public boolean accepts(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff
  ) {
    return accepts(trip);
  }

  @Override
  public boolean acceptsAccessEgress(
    CarpoolTrip trip,
    WgsCoordinate coordinateOfPassenger,
    Instant passengerDepartureTime,
    Duration searchWindow
  ) {
    return accepts(trip);
  }
}
