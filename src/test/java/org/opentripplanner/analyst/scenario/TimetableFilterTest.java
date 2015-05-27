package org.opentripplanner.analyst.scenario;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.junit.Test;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for various timetable filters
 */
public class TimetableFilterTest extends TestCase {
    private TripPattern pattern;
    private Trip trip;
    private TripTimes times;
    private FrequencyEntry frequencyEntry;
    private Stop[] stops;
    private Agency agency;
    private Route route;

    /* ROUTE REMOVAL (also matching) */

    /** test that routes that should be removed are */
    @Test
    public void testRouteRemoval () {
        RemoveTrips rt = new RemoveTrips();
        rt.agencyId = agency.getId();
        rt.routeId = Arrays.asList(route.getId().getId());

        assertNull(rt.apply(trip, pattern, times));
        assertNull(rt.apply(trip, pattern, frequencyEntry));
    }

    /** test that routes that should not be removed are not */
    @Test
    public void testRoutePreservation () {
        RemoveTrips rt = new RemoveTrips();
        rt.agencyId = agency.getId();
        rt.routeId = Arrays.asList("SOMETHING ELSE");

        assertEquals(times, rt.apply(trip, pattern, times));
        assertEquals(frequencyEntry, rt.apply(trip, pattern, frequencyEntry));
    }

    /** Test that trips that should be removed are */
    @Test
    public void testTripRemoval () {
        RemoveTrips rt = new RemoveTrips();
        rt.agencyId = agency.getId();
        rt.tripId = Arrays.asList(trip.getId().getId());

        assertNull(rt.apply(trip, pattern, times));
        assertNull(rt.apply(trip, pattern, frequencyEntry));
    }

    /** test that trips that should not be removed are not */
    @Test
    public void testTripPreservation () {
        RemoveTrips rt = new RemoveTrips();
        rt.agencyId = agency.getId();
        rt.tripId = Arrays.asList("SOMETHING ELSE");

        assertEquals(times, rt.apply(trip, pattern, times));
        assertEquals(frequencyEntry, rt.apply(trip, pattern, frequencyEntry));
    }

    /** test a highly specific match */
    @Test
    public void testSpecificRemoval () {
        // route ID and trip ID are supposed to combined with a logical and, ensure that they are.
        RemoveTrips rt = new RemoveTrips();
        rt.agencyId = agency.getId();
        rt.routeId = Arrays.asList(route.getId().getId());
        rt.tripId = Arrays.asList(trip.getId().getId());

        assertNull(rt.apply(trip, pattern, times));
        assertNull(rt.apply(trip, pattern, frequencyEntry));

        rt.routeId = Arrays.asList("SOMETHING ELSE");

        assertEquals(times, rt.apply(trip, pattern, times));
        assertEquals(frequencyEntry, rt.apply(trip, pattern, frequencyEntry));

        rt.tripId = Arrays.asList("SOMETHING ELSE");

        assertEquals(times, rt.apply(trip, pattern, times));
        assertEquals(frequencyEntry, rt.apply(trip, pattern, frequencyEntry));

        rt.routeId = Arrays.asList(route.getId().getId());

        assertEquals(times, rt.apply(trip, pattern, times));
        assertEquals(frequencyEntry, rt.apply(trip, pattern, frequencyEntry));
    }

    /** Test modification of dwell times */
    @Test
    public void testDwellTimes () {
        AdjustDwellTime adt = new AdjustDwellTime();
        adt.routeId = Arrays.asList(route.getId().getId());
        adt.agencyId = agency.getId();
        adt.stopId = Arrays.asList(stops[0].getId().getId(), stops[2].getId().getId());
        adt.dwellTime = 60;

        TripTimes tt2 = adt.apply(trip, pattern, times);
        assertNotNull(tt2);
        assertEquals(times.getArrivalTime(0), tt2.getArrivalTime(0));
        assertEquals(60, tt2.getDepartureTime(0) - tt2.getArrivalTime(0));
        assertEquals(30, tt2.getDepartureTime(1) - tt2.getArrivalTime(1));
        assertEquals(60, tt2.getDepartureTime(2) - tt2.getArrivalTime(2));
        assertEquals(30, tt2.getDepartureTime(3) - tt2.getArrivalTime(3));

        // make sure we didn't accidentally modify the orignal times
        assertEquals(30, times.getDepartureTime(2) - times.getArrivalTime(2));

        FrequencyEntry fe2 = adt.apply(trip, pattern, frequencyEntry);
        assertNotNull(fe2);

        tt2 = fe2.tripTimes;

        assertEquals(60, tt2.getDepartureTime(0) - tt2.getArrivalTime(0));
        assertEquals(30, tt2.getDepartureTime(1) - tt2.getArrivalTime(1));
        assertEquals(60, tt2.getDepartureTime(2) - tt2.getArrivalTime(2));
        assertEquals(30, tt2.getDepartureTime(3) - tt2.getArrivalTime(3));

        // make sure we didn't accidentally modify the original times
        assertEquals(30, frequencyEntry.tripTimes.getDepartureTime(2) - frequencyEntry.tripTimes.getArrivalTime(2));

        // wildcard
        adt.stopId = null;

        tt2 = adt.apply(trip, pattern, times);
        assertNotNull(tt2);
        assertEquals(times.getArrivalTime(0), tt2.getArrivalTime(0));
        assertEquals(60, tt2.getDepartureTime(0) - tt2.getArrivalTime(0));
        assertEquals(60, tt2.getDepartureTime(1) - tt2.getArrivalTime(1));
        assertEquals(60, tt2.getDepartureTime(2) - tt2.getArrivalTime(2));
        assertEquals(60, tt2.getDepartureTime(3) - tt2.getArrivalTime(3));

        // test repeated application
        adt.stopId = Arrays.asList(stops[2].getId().getId());
        adt.dwellTime = 17;

        tt2 = adt.apply(trip, pattern, tt2);
        assertNotNull(tt2);
        assertEquals(times.getArrivalTime(0), tt2.getArrivalTime(0));
        assertEquals(60, tt2.getDepartureTime(0) - tt2.getArrivalTime(0));
        assertEquals(60, tt2.getDepartureTime(1) - tt2.getArrivalTime(1));
        assertEquals(17, tt2.getDepartureTime(2) - tt2.getArrivalTime(2));
        assertEquals(60, tt2.getDepartureTime(3) - tt2.getArrivalTime(3));
    }


    @Override
    protected void setUp () {
        agency = new Agency();
        agency.setId("AGENCY");
        route = new Route();
        route.setShortName("T");
        route.setLongName("TEST");
        route.setAgency(agency);
        route.setId(new AgencyAndId(agency.getId(), "TEST"));

        trip = new Trip();
        trip.setRoute(route);
        trip.setId(new AgencyAndId(agency.getId(), "TRIP"));

        stops = new Stop[4];

        for (int i = 0; i < stops.length; i++) {
            Stop s = new Stop();
            s.setLat(-122.123);
            s.setLon(37.363 + i * 0.001);
            s.setId(new AgencyAndId(agency.getId(), "" + i));
            stops[i] = s;
        }

        StopPattern sp = new StopPattern(makeStopTimes());

        pattern = new TripPattern(route, sp);

        // make a triptimes
        times = makeTripTimes();

        // ten-minute frequency
        frequencyEntry = new FrequencyEntry(7 * 3600, 12 * 3600, 600, false, makeTripTimes());
    }

    /** Make up some trip times. Dwell is 30s, hop is 120s */
    private TripTimes makeTripTimes () {
        return new TripTimes(trip, makeStopTimes(), new Deduplicator());
    }

    private List<StopTime> makeStopTimes () {
        StopTime[] stopTimes = new StopTime[stops.length];
        int cumulativeTime = 7 * 3600;

        for (int i = 0; i < stops.length; i++) {
            Stop stop = stops[i];
            StopTime st = new StopTime();
            st.setStop(stop);
            st.setArrivalTime(cumulativeTime);
            // dwell time is 30 secs
            cumulativeTime += 30;
            st.setDepartureTime(cumulativeTime);
            // hop time is 2 minutes
            cumulativeTime += 120;

            st.setPickupType(StopPattern.PICKDROP_SCHEDULED);
            st.setDropOffType(StopPattern.PICKDROP_SCHEDULED);
            stopTimes[i] = st;
        }

        return Arrays.asList(stopTimes);
    }
}
