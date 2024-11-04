package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.transit.model.network.CarAccess;

public class CarAccessMapperTest {

  private static final int CARS_ALLOWED = 1;
  private static final int CARS_NOT_ALLOWED = 2;

  @Test
  public void testTripProvidedValues() {
    Trip trip = new Trip();
    assertEquals(CarAccess.UNKNOWN, CarAccessMapper.mapForTrip(trip));

    trip.setCarsAllowed(CARS_ALLOWED);
    assertEquals(CarAccess.ALLOWED, CarAccessMapper.mapForTrip(trip));

    trip.setCarsAllowed(CARS_NOT_ALLOWED);
    assertEquals(CarAccess.NOT_ALLOWED, CarAccessMapper.mapForTrip(trip));
  }
}
