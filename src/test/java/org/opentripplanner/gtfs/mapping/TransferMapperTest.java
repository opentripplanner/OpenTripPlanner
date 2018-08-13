package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Transfer;
import org.onebusaway.gtfs.model.Trip;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TransferMapperTest {
    private static final RouteMapper ROUTE_MAPPER = new RouteMapper(new AgencyMapper());

    private static final TripMapper TRIP_MAPPER = new TripMapper(ROUTE_MAPPER);

    private static final StopMapper STOP_MAPPER = new StopMapper();

    private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

    private static final Integer ID = 45;

    private static final Route FROM_ROUTE = new Route();

    private static final Stop FROM_STOP = new Stop();

    private static final Trip FROM_TRIP = new Trip();

    private static final Route TO_ROUTE = new Route();

    private static final Stop TO_STOP = new Stop();

    private static final Trip TO_TRIP = new Trip();

    private static final int MIN_TRANSFER_TIME = 200;

    private static final int TRANSFER_TYPE = 3;

    private static final Transfer TRANSFER = new Transfer();

    static {
        FROM_ROUTE.setId(AGENCY_AND_ID);
        FROM_STOP.setId(AGENCY_AND_ID);
        FROM_TRIP.setId(AGENCY_AND_ID);
        TO_ROUTE.setId(AGENCY_AND_ID);
        TO_STOP.setId(AGENCY_AND_ID);
        TO_TRIP.setId(AGENCY_AND_ID);

        TRANSFER.setId(ID);
        TRANSFER.setFromRoute(FROM_ROUTE);
        TRANSFER.setFromStop(FROM_STOP);
        TRANSFER.setFromTrip(FROM_TRIP);
        TRANSFER.setToRoute(TO_ROUTE);
        TRANSFER.setToStop(TO_STOP);
        TRANSFER.setToTrip(TO_TRIP);
        TRANSFER.setMinTransferTime(MIN_TRANSFER_TIME);
        TRANSFER.setTransferType(TRANSFER_TYPE);
    }

    private TransferMapper subject = new TransferMapper(ROUTE_MAPPER, STOP_MAPPER, TRIP_MAPPER);

    @Test
    public void testMapCollection() throws Exception {
        assertNull(null, subject.map((Collection<Transfer>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(Collections.singleton(TRANSFER)).size());
    }

    @Test
    public void testMap() throws Exception {
        org.opentripplanner.model.Transfer result = subject.map(TRANSFER);

        assertNotNull(result.getFromRoute());
        assertNotNull(result.getFromTrip());
        assertNotNull(result.getFromStop());
        assertNotNull(result.getToRoute());
        assertNotNull(result.getToTrip());
        assertNotNull(result.getToStop());
        assertEquals(MIN_TRANSFER_TIME, result.getMinTransferTime());
        assertEquals(TRANSFER_TYPE, result.getTransferType());
    }

    @Test
    public void testMapWithNulls() throws Exception {
        org.opentripplanner.model.Transfer result = subject.map(new Transfer());

        assertNull(result.getFromRoute());
        assertNull(result.getFromTrip());
        assertNull(result.getFromStop());
        assertNull(result.getToRoute());
        assertNull(result.getToTrip());
        assertNull(result.getToStop());
        assertFalse(result.isMinTransferTimeSet());
        assertEquals(0, result.getTransferType());
    }

    /** Mapping the same object twice, should return the the same instance. */
    @Test
    public void testMapCache() throws Exception {
        org.opentripplanner.model.Transfer result1 = subject.map(TRANSFER);
        org.opentripplanner.model.Transfer result2 = subject.map(TRANSFER);

        assertTrue(result1 == result2);
    }
}