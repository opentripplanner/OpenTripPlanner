package org.opentripplanner.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BikeAccessTest {

    public static final String FEED_ID = "F";
    private final Trip trip = new Trip(new FeedScopedId(FEED_ID, "T1"));
    private final Route route = new Route(new FeedScopedId(FEED_ID, "R1"));

    @Before
    public void setup() {
        trip.setRoute(route);
    }

    @Test
    public void testBikesAllowed() {
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
