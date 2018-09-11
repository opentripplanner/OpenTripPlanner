package org.opentripplanner.gtfs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

public class BikeAccessTest {

    @Test
    public void testBikesAllowed() {
        Trip trip = new Trip();
        Route route = new Route();
        trip.setRoute(route);

        assertEquals(BikeAccess.UNKNOWN, BikeAccess.fromTrip(trip));
        trip.setBikesAllowed(1);
        assertEquals(BikeAccess.ALLOWED, BikeAccess.fromTrip(trip));
        trip.setBikesAllowed(2);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
        route.setBikesAllowed(1);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
        trip.setBikesAllowed(0);
        assertEquals(BikeAccess.ALLOWED, BikeAccess.fromTrip(trip));
        route.setBikesAllowed(2);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testTripBikesAllowed() {
        Trip trip = new Trip();
        Route route = new Route();
        trip.setRoute(route);

        assertEquals(BikeAccess.UNKNOWN, BikeAccess.fromTrip(trip));
        trip.setTripBikesAllowed(2);
        assertEquals(BikeAccess.ALLOWED, BikeAccess.fromTrip(trip));
        trip.setTripBikesAllowed(1);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
        route.setRouteBikesAllowed(2);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
        trip.setTripBikesAllowed(0);
        assertEquals(BikeAccess.ALLOWED, BikeAccess.fromTrip(trip));
        route.setRouteBikesAllowed(1);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBikesAllowedOverridesTripBikesAllowed() {
        Trip trip = new Trip();
        Route route = new Route();
        trip.setRoute(route);

        trip.setBikesAllowed(1);
        trip.setTripBikesAllowed(1);
        assertEquals(BikeAccess.ALLOWED, BikeAccess.fromTrip(trip));
        trip.setBikesAllowed(2);
        trip.setTripBikesAllowed(2);
        assertEquals(BikeAccess.NOT_ALLOWED, BikeAccess.fromTrip(trip));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void setBikesAllowed() {
        Trip trip = new Trip();
        BikeAccess.setForTrip(trip, BikeAccess.ALLOWED);
        assertEquals(1, trip.getBikesAllowed());
        assertEquals(2, trip.getTripBikesAllowed());
        BikeAccess.setForTrip(trip, BikeAccess.NOT_ALLOWED);
        assertEquals(2, trip.getBikesAllowed());
        assertEquals(1, trip.getTripBikesAllowed());
        BikeAccess.setForTrip(trip, BikeAccess.UNKNOWN);
        assertEquals(0, trip.getBikesAllowed());
        assertEquals(0, trip.getTripBikesAllowed());
    }
}
