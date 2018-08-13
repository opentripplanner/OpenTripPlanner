package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StopTimeMapperTest {
    private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

    private static final Integer ID = 45;

    private static final int ARRIVAL_TIME = 1000;

    private static final int DEPARTURE_TIME = 2000;

    private static final int DROP_OFF_TYPE = 2;

    private static final String FARE_PERIOD_ID = "Fare Period Id";

    private static final int PICKUP_TYPE = 3;

    private static final String ROUTE_SHORT_NAME = "Route Short Name";

    private static final double SHAPE_DIST_TRAVELED = 2.5d;

    private static final Stop STOP = new Stop();

    private static final String HEAD_SIGN = "Head Sign";

    private static final int STOP_SEQUENCE = 4;

    private static final int TIMEPOINT = 50;

    private static final Trip TRIP = new Trip();

    private static final StopTime STOP_TIME = new StopTime();

    static {
        TRIP.setId(AGENCY_AND_ID);
        STOP.setId(AGENCY_AND_ID);

        STOP_TIME.setId(ID);
        STOP_TIME.setArrivalTime(ARRIVAL_TIME);
        STOP_TIME.setDepartureTime(DEPARTURE_TIME);
        STOP_TIME.setDropOffType(DROP_OFF_TYPE);
        STOP_TIME.setFarePeriodId(FARE_PERIOD_ID);
        STOP_TIME.setPickupType(PICKUP_TYPE);
        STOP_TIME.setRouteShortName(ROUTE_SHORT_NAME);
        STOP_TIME.setShapeDistTraveled(SHAPE_DIST_TRAVELED);
        STOP_TIME.setStop(STOP);
        STOP_TIME.setStopHeadsign(HEAD_SIGN);
        STOP_TIME.setStopSequence(STOP_SEQUENCE);
        STOP_TIME.setTimepoint(TIMEPOINT);
        STOP_TIME.setTrip(TRIP);
    }

    private StopTimeMapper subject = new StopTimeMapper(
            new StopMapper(), new TripMapper(new RouteMapper(new AgencyMapper()))
    );

    @Test
    public void testMapCollection() throws Exception {
        assertNull(null, subject.map((Collection<StopTime>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(Collections.singleton(STOP_TIME)).size());
    }

    @Test
    public void testMap() throws Exception {
        org.opentripplanner.model.StopTime result = subject.map(STOP_TIME);

        assertEquals(ARRIVAL_TIME, result.getArrivalTime());
        assertEquals(DEPARTURE_TIME, result.getDepartureTime());
        assertEquals(DROP_OFF_TYPE, result.getDropOffType());
        assertEquals(FARE_PERIOD_ID, result.getFarePeriodId());
        assertEquals(PICKUP_TYPE, result.getPickupType());
        assertEquals(ROUTE_SHORT_NAME, result.getRouteShortName());
        assertEquals(SHAPE_DIST_TRAVELED, result.getShapeDistTraveled(), 0.0001d);
        assertNotNull(result.getStop());
        assertEquals(HEAD_SIGN, result.getStopHeadsign());
        assertEquals(STOP_SEQUENCE, result.getStopSequence());
        assertEquals(TIMEPOINT, result.getTimepoint());
        assertNotNull(result.getTrip());
    }

    @Test
    public void testMapWithNulls() throws Exception {
        org.opentripplanner.model.StopTime result = subject.map(new StopTime());

        assertFalse(result.isArrivalTimeSet());
        assertFalse(result.isDepartureTimeSet());
        assertEquals(0, result.getDropOffType());
        assertNull(result.getFarePeriodId());
        assertEquals(0, result.getPickupType());
        assertNull(result.getRouteShortName());
        assertFalse(result.isShapeDistTraveledSet());
        assertNull(result.getStop());
        assertNull(result.getStopHeadsign());
        assertEquals(0, result.getStopSequence());
        assertFalse(result.isTimepointSet());
    }

    /** Mapping the same object twice, should return the the same instance. */
    @Test
    public void testMapCache() throws Exception {
        org.opentripplanner.model.StopTime result1 = subject.map(STOP_TIME);
        org.opentripplanner.model.StopTime result2 = subject.map(STOP_TIME);

        assertTrue(result1 == result2);
    }
}