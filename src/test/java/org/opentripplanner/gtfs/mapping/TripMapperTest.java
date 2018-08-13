package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TripMapperTest {

    private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

    private static final int BIKES_ALLOWED = 1;

    private static final String BLOCK_ID = "Block Id";

    private static final String DIRECTION_ID = "Direction Id";

    private static final String FARE_ID = "Fare Id";

    private static final Route ROUTE = new Route();

    private static final String ROUTE_SHORT_NAME = "Route Short Name";

    private static final String TRIP_HEADSIGN = "Trip Headsign";

    private static final String TRIP_SHORT_NAME = "Trip Short Name";

    private static final int WHEELCHAIR_ACCESSIBLE = 2;

    private static final int TRIP_BIKES_ALLOWED = 2;

    private static final Trip TRIP = new Trip();

    static {
        ROUTE.setId(AGENCY_AND_ID);

        TRIP.setId(AGENCY_AND_ID);
        TRIP.setBikesAllowed(BIKES_ALLOWED);
        TRIP.setBlockId(BLOCK_ID);
        TRIP.setDirectionId(DIRECTION_ID);
        TRIP.setFareId(FARE_ID);
        TRIP.setRoute(ROUTE);
        TRIP.setRouteShortName(ROUTE_SHORT_NAME);
        TRIP.setServiceId(AGENCY_AND_ID);
        TRIP.setShapeId(AGENCY_AND_ID);
        TRIP.setTripHeadsign(TRIP_HEADSIGN);
        TRIP.setTripShortName(TRIP_SHORT_NAME);
        TRIP.setWheelchairAccessible(WHEELCHAIR_ACCESSIBLE);
        TRIP.setTripBikesAllowed(TRIP_BIKES_ALLOWED);
    }

    private TripMapper subject = new TripMapper(new RouteMapper(new AgencyMapper()));

    @Test
    public void testMapCollection() throws Exception {
        assertNull(subject.map((Collection<Trip>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(Collections.singleton(TRIP)).size());
    }

    @Test
    public void testMap() throws Exception {
        org.opentripplanner.model.Trip result = subject.map(TRIP);

        assertEquals("A_1", result.getId().toString());
        assertEquals(BIKES_ALLOWED, result.getBikesAllowed());
        assertEquals(BLOCK_ID, result.getBlockId());
        assertEquals(DIRECTION_ID, result.getDirectionId());
        assertEquals(FARE_ID, result.getFareId());
        assertNotNull(result.getRoute());
        assertEquals(ROUTE_SHORT_NAME, result.getRouteShortName());
        assertEquals("A_1", result.getServiceId().toString());
        assertEquals("A_1", result.getShapeId().toString());
        assertEquals(TRIP_HEADSIGN, result.getTripHeadsign());
        assertEquals(TRIP_SHORT_NAME, result.getTripShortName());
        assertEquals(WHEELCHAIR_ACCESSIBLE, result.getWheelchairAccessible());
        assertEquals(TRIP_BIKES_ALLOWED, result.getTripBikesAllowed());
    }

    @Test
    public void testMapWithNulls() throws Exception {
        Trip input = new Trip();
        input.setId(AGENCY_AND_ID);

        org.opentripplanner.model.Trip result = subject.map(input);

        assertNotNull(result.getId());
        assertEquals(0, result.getBikesAllowed());
        assertNull(result.getBlockId());
        assertNull(result.getDirectionId());
        assertNull(result.getFareId());
        assertNull(result.getRoute());
        assertNull(result.getRouteShortName());
        assertNull(result.getServiceId());
        assertNull(result.getShapeId());
        assertNull(result.getTripHeadsign());
        assertNull(result.getTripShortName());
        assertEquals(0, result.getWheelchairAccessible());
        assertEquals(0, result.getTripBikesAllowed());
    }

    /** Mapping the same object twice, should return the the same instance. */
    @Test
    public void testMapCache() throws Exception {
        org.opentripplanner.model.Trip result1 = subject.map(TRIP);
        org.opentripplanner.model.Trip result2 = subject.map(TRIP);

        assertTrue(result1 == result2);
    }
}