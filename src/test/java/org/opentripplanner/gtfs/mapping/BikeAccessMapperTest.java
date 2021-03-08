package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.model.BikeAccess;

import static org.junit.Assert.assertEquals;

public class BikeAccessMapperTest {

    private static final int BIKES_ALLOWED = 1;
    private static final int BIKES_NOT_ALLOWED = 2;
    private static final int TRIP_BIKES_ALLOWED = 2;
    private static final int TRIP_BIKES_NOT_ALLOWED = 1;
    private static final int ROUTE_BIKES_ALLOWED = 2;
    private static final int ROUTE_BIKES_NOT_ALLOWED = 1;

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
    public void testLegacyTripProvidedValues() {
        Trip trip = new Trip();
        assertEquals(BikeAccess.UNKNOWN, BikeAccessMapper.mapForTrip(trip));

        trip.setTripBikesAllowed(TRIP_BIKES_ALLOWED);
        assertEquals(BikeAccess.ALLOWED, BikeAccessMapper.mapForTrip(trip));

        trip.setTripBikesAllowed(TRIP_BIKES_NOT_ALLOWED);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccessMapper.mapForTrip(trip));
    }

    @Test
    public void testTripProvidedValuesPrecedence() {
        Trip trip = new Trip();
        assertEquals(BikeAccess.UNKNOWN, BikeAccessMapper.mapForTrip(trip));

        trip.setBikesAllowed(BIKES_ALLOWED);
        trip.setTripBikesAllowed(TRIP_BIKES_NOT_ALLOWED);
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

    @Test
    public void testLegacyRouteProvidedValues() {
        Route route = new Route();
        assertEquals(BikeAccess.UNKNOWN, BikeAccessMapper.mapForRoute(route));

        route.setRouteBikesAllowed(ROUTE_BIKES_ALLOWED);
        assertEquals(BikeAccess.ALLOWED, BikeAccessMapper.mapForRoute(route));

        route.setRouteBikesAllowed(ROUTE_BIKES_NOT_ALLOWED);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccessMapper.mapForRoute(route));
    }

    @Test
    public void testRouteProvidedValuesPrecedence() {
        Route route = new Route();
        assertEquals(BikeAccess.UNKNOWN, BikeAccessMapper.mapForRoute(route));

        route.setBikesAllowed(BIKES_ALLOWED);
        route.setRouteBikesAllowed(ROUTE_BIKES_NOT_ALLOWED);
        assertEquals(BikeAccess.ALLOWED, BikeAccessMapper.mapForRoute(route));
    }
}