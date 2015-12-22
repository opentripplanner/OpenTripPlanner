package org.opentripplanner.updater;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import org.opentripplanner.GtfsTest;

public class GtfsRealtimeFuzzyTripMatcherTest extends GtfsTest {

    public void testMatch() throws Exception {
        String feedId = graph.getFeedIds().iterator().next();

        GtfsRealtimeFuzzyTripMatcher matcher = new GtfsRealtimeFuzzyTripMatcher(graph.index);
        TripDescriptor trip1 = TripDescriptor.newBuilder().setRouteId("1").setDirectionId(0).
                setStartTime("06:47:00").setStartDate("20090915").build();
        assertEquals("10W1020", matcher.match(feedId, trip1).getTripId());
        trip1 = TripDescriptor.newBuilder().setRouteId("4").setDirectionId(0).
                setStartTime("00:02:00").setStartDate("20090915").build();
        assertEquals("40W1890", matcher.match(feedId, trip1).getTripId());
        // Test matching with "real time", when schedule uses time grater than 24:00
        trip1 = TripDescriptor.newBuilder().setRouteId("4").setDirectionId(0).
                setStartTime("12:00:00").setStartDate("20090915").build();
        // No departure at this time
        assertFalse(trip1.hasTripId());
        trip1 = TripDescriptor.newBuilder().setRouteId("1").
                setStartTime("06:47:00").setStartDate("20090915").build();
        // Missing direction id
        assertFalse(trip1.hasTripId());
    }

    @Override
    public String getFeedName() {
        return "google_transit.zip";
    }
}