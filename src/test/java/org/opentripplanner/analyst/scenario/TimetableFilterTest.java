package org.opentripplanner.analyst.scenario;

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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Tests for various timetable filters
 */
public class TimetableFilterTest extends TestCase {
    private TripPattern pattern, metroPattern;
    private Trip trip, trip2, metroTrip;
    private TripTimes times, metroTimes;
    private FrequencyEntry frequencyEntry;
    private Stop[] stops;
    private Agency agency;
    private Route route, metro;

    /* ROUTE REMOVAL (also matching) */

    /** test that routes that should be removed are */
    @Test
    public void testRouteRemoval () {
        RemoveTrip rt = new RemoveTrip();
        rt.agencyId = agency.getId();
        rt.routeId = Arrays.asList(route.getId().getId());

        assertNull(rt.apply(trip, pattern, times));
        assertNull(rt.apply(trip, pattern, frequencyEntry));
    }

    /** test that routes removed by route type work */
    @Test
    public void testTripRemovalByRouteType () {
        RemoveTrip rt = new RemoveTrip();
        rt.agencyId = agency.getId();
        rt.routeType = new int[] { com.conveyal.gtfs.model.Route.SUBWAY };

        assertNull(rt.apply(metroTrip, metroPattern, metroTimes));
        assertNotNull(rt.apply(trip, pattern, times));
    }

    /** test that routes that should not be removed are not */
    @Test
    public void testRoutePreservation () {
        RemoveTrip rt = new RemoveTrip();
        rt.agencyId = agency.getId();
        rt.routeId = Arrays.asList("SOMETHING ELSE");

        assertEquals(times, rt.apply(trip, pattern, times));
        assertEquals(frequencyEntry, rt.apply(trip, pattern, frequencyEntry));
    }

    /** Test that trips that should be removed are */
    @Test
    public void testTripRemoval () {
        RemoveTrip rt = new RemoveTrip();
        rt.agencyId = agency.getId();
        rt.tripId = Arrays.asList(trip.getId().getId());

        assertNull(rt.apply(trip, pattern, times));
        assertNull(rt.apply(trip, pattern, frequencyEntry));
    }

    /** test that trips that should not be removed are not */
    @Test
    public void testTripPreservation () {
        RemoveTrip rt = new RemoveTrip();
        rt.agencyId = agency.getId();
        rt.tripId = Arrays.asList("SOMETHING ELSE");

        assertEquals(times, rt.apply(trip, pattern, times));
        assertEquals(frequencyEntry, rt.apply(trip, pattern, frequencyEntry));

        rt.agencyId = "NOT THE AGENCY ID";
        rt.tripId = Arrays.asList(trip.getId().getId());

        assertEquals(times, rt.apply(trip, pattern, times));
        assertEquals(frequencyEntry, rt.apply(trip, pattern, frequencyEntry));
    }

    /** test a highly specific match */
    @Test
    public void testSpecificRemoval () {
        // route ID and trip ID are supposed to combined with a logical and, ensure that they are.
        RemoveTrip rt = new RemoveTrip();
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

    /** test modifying frequencies */
    @Test
    public void testAdjustHeadway () {
        AdjustHeadway ah = new AdjustHeadway();
        ah.agencyId = agency.getId();
        ah.routeId = Arrays.asList(route.getId().getId());
        ah.headway = 120;

        // should have no effect on scheduled trips
        assertEquals(times, ah.apply(trip, pattern, times));

        FrequencyEntry fe2 = ah.apply(trip, pattern, frequencyEntry);
        assertNotNull(fe2);
        assertEquals(120, fe2.headway);
        // make sure we didn't accidentally modify the entry in the graph
        assertEquals(600, frequencyEntry.headway);
    }

    /** test modifying trip patterns */
    @Test
    public void testSkipStopInMiddle () {
        SkipStop ss = new SkipStop();
        ss.routeId = Arrays.asList(route.getId().getId());
        ss.agencyId = agency.getId();
        ss.stopId = Arrays.asList(stops[2].getId().getId());

        Collection<TripPattern> result = ss.apply(pattern);

        assertEquals(1, result.size());

        TripPattern newtp = result.iterator().next();

        assertNotSame(pattern, newtp);

        // TODO getNumScheduledTrips is zero - why?
        assertEquals(2, newtp.scheduledTimetable.tripTimes.size());
        assertEquals(2, newtp.scheduledTimetable.frequencyEntries.size());

        assertEquals(3, newtp.stopPattern.size);

        // make sure the times are correct
        assertEquals(pattern.scheduledTimetable.tripTimes.get(0).getDepartureTime(0),
                newtp.scheduledTimetable.tripTimes.get(0).getDepartureTime(0));
        // after the skipped stop: dwell times should be removed
        assertEquals(pattern.scheduledTimetable.tripTimes.get(0).getDepartureTime(3) - 30,
                newtp.scheduledTimetable.tripTimes.get(0).getDepartureTime(2));

        assertEquals(pattern.stopPattern.stops[3], newtp.stopPattern.stops[2]);

        // and for the frequency entry
        // make sure the times are correct
        assertEquals(pattern.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(0),
                newtp.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(0));
        // after the skipped stop: dwell times should be removed
        assertEquals(pattern.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(3) - 30,
                newtp.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(2));
    }

    /** test modifying one trip on a trip pattern */
    @Test
    public void testModifySingleTrip  () {
        SkipStop ss = new SkipStop();
        ss.routeId = Arrays.asList(route.getId().getId());
        ss.agencyId = agency.getId();
        ss.tripId = Arrays.asList(trip.getId().getId());
        ss.stopId = Arrays.asList(stops[2].getId().getId());

        Collection<TripPattern> result = ss.apply(pattern);

        assertEquals(2, result.size());

        Iterator<TripPattern> tpit = result.iterator();
        // non-ideal: assuming defined order.
        TripPattern newtp = tpit.next();
        TripPattern clone = tpit.next();

        assertNotSame(pattern, newtp);
        assertNotSame(pattern, clone);

        assertEquals(1, newtp.scheduledTimetable.tripTimes.size());
        assertEquals(1, newtp.scheduledTimetable.frequencyEntries.size());

        assertEquals(3, newtp.stopPattern.size);

        // make sure the times are correct
        assertEquals(pattern.scheduledTimetable.tripTimes.get(0).getDepartureTime(0),
                newtp.scheduledTimetable.tripTimes.get(0).getDepartureTime(0));
        // after the skipped stop: dwell times should be removed
        assertEquals(pattern.scheduledTimetable.tripTimes.get(0).getDepartureTime(3) - 30,
                newtp.scheduledTimetable.tripTimes.get(0).getDepartureTime(2));

        assertEquals(pattern.stopPattern.stops[3], newtp.stopPattern.stops[2]);

        // and for the frequency entry
        // make sure the times are correct
        assertEquals(pattern.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(0),
                newtp.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(0));
        // after the skipped stop: dwell times should be removed
        assertEquals(pattern.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(3) - 30,
                newtp.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(2));

        // make sure the times are correct on the trips that were not modified
        assertEquals(pattern.scheduledTimetable.tripTimes.get(0).getDepartureTime(0),
                clone.scheduledTimetable.tripTimes.get(0).getDepartureTime(0));
        // after the skipped stop: dwell times should be removed
        assertEquals(pattern.scheduledTimetable.tripTimes.get(0).getDepartureTime(3),
                clone.scheduledTimetable.tripTimes.get(0).getDepartureTime(3));

        assertEquals(pattern.stopPattern.stops[3], clone.stopPattern.stops[3]);

        // and for the frequency entry
        // make sure the times are correct
        assertEquals(pattern.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(0),
                clone.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(0));
        // after the skipped stop: dwell times should be removed
        assertEquals(pattern.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(3),
                clone.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(3));
    }
    @Test
    public void testSkipStopsAtStart () {
        SkipStop ss = new SkipStop();
        ss.routeId = Arrays.asList(route.getId().getId());
        ss.agencyId = agency.getId();
        ss.stopId = Arrays.asList(stops[0].getId().getId(), stops[1].getId().getId());

        Collection<TripPattern> result = ss.apply(pattern);

        assertEquals(1, result.size());

        TripPattern newtp = result.iterator().next();

        assertNotSame(pattern, newtp);

        assertEquals(2, newtp.scheduledTimetable.tripTimes.size());
        assertEquals(2, newtp.scheduledTimetable.frequencyEntries.size());

        assertEquals(2, newtp.stopPattern.size);

        // make sure the times are correct
        // Note that there should be no dwell compression; the start of the trip should simply be chopped off.
        assertEquals(pattern.scheduledTimetable.getTripTimes(0).getDepartureTime(2),
                newtp.scheduledTimetable.getTripTimes(0).getDepartureTime(0));
        assertEquals(pattern.scheduledTimetable.getTripTimes(0).getDepartureTime(3),
                newtp.scheduledTimetable.getTripTimes(0).getDepartureTime(1));

        assertEquals(pattern.stopPattern.stops[3], newtp.stopPattern.stops[1]);

        // and for the frequency entry
        // make sure the times are correct
        assertEquals(pattern.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(2),
                newtp.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(0));
        // after the skipped stop: dwell times should be removed
        assertEquals(pattern.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(3),
                newtp.scheduledTimetable.frequencyEntries.get(0).tripTimes.getDepartureTime(1));
    }

    @Override
    protected void setUp () {
        agency = new Agency();
        agency.setId("AGENCY");
        route = new Route();
        route.setType(com.conveyal.gtfs.model.Route.BUS);
        route.setShortName("T");
        route.setLongName("TEST");
        route.setAgency(agency);
        route.setId(new AgencyAndId(agency.getId(), "TEST"));

        metro = new Route();
        metro.setType(com.conveyal.gtfs.model.Route.SUBWAY);
        metro.setShortName("M");
        metro.setLongName("METRO");
        metro.setAgency(agency);
        metro.setId(new AgencyAndId(agency.getId(), "METRO"));

        trip = new Trip();
        trip.setRoute(route);
        trip.setId(new AgencyAndId(agency.getId(), "TRIP"));

        trip2 = new Trip();
        trip2.setRoute(route);
        trip2.setId(new AgencyAndId(agency.getId(), "TRIP2"));


        stops = new Stop[4];

        for (int i = 0; i < stops.length; i++) {
            Stop s = new Stop();
            s.setLat(-122.123);
            s.setLon(37.363 + i * 0.001);
            s.setId(new AgencyAndId(agency.getId(), "" + i));
            stops[i] = s;
        }

        List<StopTime> stopTimes = makeStopTimes(trip);
        StopPattern sp = new StopPattern(stopTimes);

        pattern = new TripPattern(route, sp);

        // make a triptimes
        times = makeTripTimes(trip, stopTimes);
        pattern.scheduledTimetable.addTripTimes(times);
        pattern.scheduledTimetable.addTripTimes(makeTripTimes(trip2, makeStopTimes(trip2)));

        // ten-minute frequency
        frequencyEntry = new FrequencyEntry(7 * 3600, 12 * 3600, 600, false, makeTripTimes(trip, makeStopTimes(trip)));
        pattern.scheduledTimetable.addFrequencyEntry(frequencyEntry);
        pattern.scheduledTimetable.addFrequencyEntry(new FrequencyEntry(7 * 3600, 12 * 3600, 600, false, makeTripTimes(trip2, makeStopTimes(trip2))));

        metroTrip = new Trip();
        metroTrip.setRoute(metro);
        metroTrip.setId(new AgencyAndId(agency.getId(), "TRIP"));

        stopTimes = makeStopTimes(metroTrip);
        sp = new StopPattern(stopTimes);

        metroPattern = new TripPattern(metro, sp);
        metroTimes = makeTripTimes(metroTrip, stopTimes);
        metroPattern.scheduledTimetable.addTripTimes(metroTimes);
    }

    /** Make up some trip times. Dwell is 30s, hop is 120s */
    private TripTimes makeTripTimes (Trip trip, List<StopTime> stopTimes) {
        return new TripTimes(trip, makeStopTimes(trip), new Deduplicator());
    }

    private List<StopTime> makeStopTimes (Trip trip) {
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

            st.setTrip(trip);

            stopTimes[i] = st;
        }

        return Arrays.asList(stopTimes);
    }
}
