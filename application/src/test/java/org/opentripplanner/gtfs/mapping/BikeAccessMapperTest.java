package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.transit.model.network.BikeAccess;

public class BikeAccessMapperTest {

  private static final int BIKES_ALLOWED = 1;
  private static final int BIKES_NOT_ALLOWED = 2;

  @Test
  public void testTripProvidedValues() {
    Trip trip = new Trip();
    assertEquals(BikeAccess.UNKNOWN, BikeAccessMapper.mapForTrip(trip));

    trip.setBikesAllowed(BIKES_ALLOWED);
    assertEquals(BikeAccess.ALLOWED, BikeAccessMapper.mapForTrip(trip));

    trip.setBikesAllowed(BIKES_NOT_ALLOWED);
    assertEquals(BikeAccess.NOT_ALLOWED, BikeAccessMapper.mapForTrip(trip));
  }

  @Test
  public void testTripProvidedValuesPrecedence() {
    Trip trip = new Trip();
    assertEquals(BikeAccess.UNKNOWN, BikeAccessMapper.mapForTrip(trip));

    trip.setBikesAllowed(BIKES_ALLOWED);
    assertEquals(BikeAccess.ALLOWED, BikeAccessMapper.mapForTrip(trip));
  }

  @Test
  public void testRouteProvidedValues() {
    Route route = new Route();
    assertEquals(BikeAccess.UNKNOWN, BikeAccessMapper.mapForRoute(route));

    route.setBikesAllowed(BIKES_ALLOWED);
    assertEquals(BikeAccess.ALLOWED, BikeAccessMapper.mapForRoute(route));

    route.setBikesAllowed(BIKES_NOT_ALLOWED);
    assertEquals(BikeAccess.NOT_ALLOWED, BikeAccessMapper.mapForRoute(route));
  }
}
