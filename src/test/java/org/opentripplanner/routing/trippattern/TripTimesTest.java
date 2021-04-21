package org.opentripplanner.routing.trippattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.mockito.Matchers;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.gtfs.BikeAccess;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SimpleConcreteVertex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.request.BannedStopSet;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

import static org.mockito.Mockito.*;

public class TripTimesTest {
    private static final FeedScopedId tripId = new FeedScopedId("agency", "testtrip");

    private static final FeedScopedId stop_a = new FeedScopedId("agency", "A"); // 0
    private static final FeedScopedId stop_b = new FeedScopedId("agency", "B"); // 1
    private static final FeedScopedId stop_c = new FeedScopedId("agency", "C"); // 2
    private static final FeedScopedId stop_d = new FeedScopedId("agency", "D"); // 3
    private static final FeedScopedId stop_e = new FeedScopedId("agency", "E"); // 4
    private static final FeedScopedId stop_f = new FeedScopedId("agency", "F"); // 5
    private static final FeedScopedId stop_g = new FeedScopedId("agency", "G"); // 6
    private static final FeedScopedId stop_h = new FeedScopedId("agency", "H"); // 7

    private static final FeedScopedId[] stops =
        {stop_a, stop_b, stop_c, stop_d, stop_e, stop_f, stop_g, stop_h};

    private static final TripTimes originalTripTimes;

    static {
        Trip trip = new Trip();
        trip.setId(tripId);

        List<StopTime> stopTimes = new LinkedList<StopTime>();

        for(int i =  0; i < stops.length; ++i) {
            StopTime stopTime = new StopTime();

            Stop stop = new Stop();
            stop.setId(stops[i]);
            stopTime.setStop(stop);
            stopTime.setArrivalTime(i * 60);
            stopTime.setDepartureTime(i * 60);
            stopTime.setStopSequence(i);
            stopTimes.add(stopTime);
        }

        originalTripTimes = new TripTimes(trip, stopTimes, new Deduplicator());
    }

    @Test
    public void testBikesAllowed() {
        Graph graph = new Graph();
        Trip trip = new Trip();
        Route route = new Route();
        trip.setRoute(route);
        List<StopTime> stopTimes = Arrays.asList(new StopTime(), new StopTime());
        TripTimes s = new TripTimes(trip, stopTimes, new Deduplicator());

        RoutingRequest request = new RoutingRequest(TraverseMode.BICYCLE);
        Vertex v = new SimpleConcreteVertex(graph, "", 0.0, 0.0);
        request.setRoutingContext(graph, v, v);
        State s0 = new State(request);

        assertFalse(s.tripAcceptable(s0, 0));

        BikeAccess.setForTrip(trip, BikeAccess.ALLOWED);
        assertTrue(s.tripAcceptable(s0, 0));

        BikeAccess.setForTrip(trip, BikeAccess.NOT_ALLOWED);
        assertFalse(s.tripAcceptable(s0, 0));
    }

    /**
     * Some tests for checking if a trip sequence should be banned. A lot of the state creation in here lacks full data
     * representation and instead contains the minimum amount of data needed to test the
     * {@link TripTimes#tripOrTripSequenceIsBanned(State, int)} method.
     */
    @Test
    public void testTripOrTripSequenceIsBanned() {
        Graph graph = new Graph();
        String agencyId = "mock agency";

        // make some generic trips for testing
        Trip tripA = new Trip();
        FeedScopedId tripAId = new FeedScopedId(agencyId, "A");
        tripA.setId(tripAId);
        Trip tripB = new Trip();
        FeedScopedId tripBId = new FeedScopedId(agencyId, "B");
        tripB.setId(tripBId);
        Trip tripC = new Trip();
        FeedScopedId tripCId = new FeedScopedId(agencyId, "C");
        tripC.setId(tripCId);
        Route route = new Route();
        tripA.setRoute(route);

        // stop A
        Stop stopA = new Stop();
        FeedScopedId stopAId = new FeedScopedId(agencyId, "A");
        stopA.setId(stopAId);
        StopTime stopTime1 = new StopTime();
        stopTime1.setStop(stopA);
        TransitStop transitStopA = new TransitStop(graph, stopA);
        TransitStopDepart departingStopA = new TransitStopDepart(graph, stopA, transitStopA);
        TransitStopArrive arrivingStopA = new TransitStopArrive(graph, stopA, transitStopA);

        // stop B
        Stop stopB = new Stop();
        FeedScopedId stopBId = new FeedScopedId(agencyId, "B");
        stopA.setId(stopBId);
        StopTime stopTime2 = new StopTime();
        stopTime2.setStop(stopB);

        // pattern 1
        TripPattern tripPattern1 = new TripPattern(route, new StopPattern(Arrays.asList(stopTime1, stopTime2)));
        PatternDepartVertex patternDepartVertex1 = new PatternDepartVertex(graph, tripPattern1, 0);
        tripPattern1.departVertices[0] = patternDepartVertex1;
        PatternArriveVertex patternArriveVertex1 = new PatternArriveVertex(graph, tripPattern1, 0);
        tripPattern1.arriveVertices[1] = patternArriveVertex1;

        // pattern 2
        TripPattern tripPattern2 = new TripPattern(route, new StopPattern(Arrays.asList(stopTime1, stopTime2)));
        PatternDepartVertex patternDepartVertex2 = new PatternDepartVertex(graph, tripPattern2, 0);
        tripPattern2.departVertices[0] = patternDepartVertex2;

        // initialize request and first state as starting a departure from a transit stop
        RoutingRequest request = new RoutingRequest(TraverseMode.WALK);
        Vertex v = new SimpleConcreteVertex(graph, "", 0.0, 0.0);
        request.setRoutingContext(graph, departingStopA, v);
        State s0 = new State(request);

        // make a generic StopTimes instance for testing
        List<StopTime> stopTimes = Arrays.asList(new StopTime(), new StopTime());

        // trip should not be banned if there aren't any banned trips or trip sequences
        TripTimes tripTimesForTripA = new TripTimes(tripA, stopTimes, new Deduplicator());
        assertFalse(tripTimesForTripA.tripOrTripSequenceIsBanned(s0, 0));

        // trip should be banned if exactly one of the trips is banned
        request.bannedTrips.put(tripAId, BannedStopSet.ALL);
        assertTrue(tripTimesForTripA.tripOrTripSequenceIsBanned(s0, 0));

        // trip should not be banned if only part of a banned sequence would exist
        request.bannedTrips.clear();
        request.bannedTripSequences.add(Arrays.asList(tripAId, tripBId));
        assertFalse(tripTimesForTripA.tripOrTripSequenceIsBanned(s0, 0));

        // board a pattern
        StateEditor s1e = s0.edit(new TransitBoardAlight(
            departingStopA,
            patternDepartVertex1,
            0,
            TraverseMode.BUS
        ));
        // trip sequence after this is now just [tripA]
        s1e.setTripTimes(new TripTimes(tripA, Arrays.asList(stopTime1), new Deduplicator()));
        State s1 = s1e.makeState();

        // trip should not be banned for sequence with additional trip not in banned sequence
        TripTimes tripTimesForTripC = new TripTimes(tripC, stopTimes, new Deduplicator());
        assertFalse(tripTimesForTripC.tripOrTripSequenceIsBanned(s1, 0));

        // trip should be banned if exact banned trip sequence occurs
        TripTimes tripTimesForTripB = new TripTimes(tripB, stopTimes, new Deduplicator());
        assertTrue(tripTimesForTripB.tripOrTripSequenceIsBanned(s1, 0));

        // trip should be banned in non-continuous sequence
        // add to state, first add a pattern hop
        StateEditor s2e = s1.edit(new PatternHop(patternDepartVertex1, patternArriveVertex1, stopA, stopB, 0));
        State s2 = s2e.makeState();
        // at this stop, the route changes to another route with tripC (as indicated by the PatternInterlineDwell edge)
        StateEditor s3e = s2.edit(new PatternInterlineDwell(tripPattern1, tripPattern2));
        // trip sequence after this is now [tripA -> tripC]
        s3e.setTripTimes(new TripTimes(tripC, Arrays.asList(stopTime1), new Deduplicator()));
        State s3 = s3e.makeState();
        assertTrue(tripTimesForTripB.tripOrTripSequenceIsBanned(s3, 0));

        // arriveBy tests
        // initialize new arriveByRequest
        RoutingRequest arriveByRequest = new RoutingRequest(TraverseMode.WALK);
        arriveByRequest.setRoutingContext(graph, arrivingStopA, v);
        arriveByRequest.setArriveBy(true);
        State as0 = new State(arriveByRequest);
        arriveByRequest.bannedTripSequences.add(Arrays.asList(tripAId, tripBId));

        // trip should not be banned if only part of a banned sequence would exist
        arriveByRequest.bannedTripSequences.add(Arrays.asList(tripAId, tripBId));
        assertFalse(tripTimesForTripB.tripOrTripSequenceIsBanned(as0, 0));

        // alight a pattern
        StateEditor as1e = as0.edit(new TransitBoardAlight(
            patternArriveVertex1,
            arrivingStopA,
            0,
            TraverseMode.BUS
        ));
        // trip sequence after this is now just [tripB]
        as1e.setTripTimes(new TripTimes(tripB, Arrays.asList(stopTime1), new Deduplicator()));
        State as1 = as1e.makeState();

        // trip should not be banned for sequence with additional trip not in banned sequence
        assertFalse(tripTimesForTripC.tripOrTripSequenceIsBanned(as1, 0));

        // trip should be banned if exact banned trip sequence occurs
        assertTrue(tripTimesForTripA.tripOrTripSequenceIsBanned(as1, 0));

        // trip should be banned in non-continuous sequence
        // first add a pattern hop
        StateEditor as2e = as1.edit(new PatternHop(patternDepartVertex2, patternArriveVertex1, stopA, stopB, 0));
        State as2 = as2e.makeState();
        // at this stop, the route changes to another route with tripB (as indicated by the PatternInterlineDwell edge)
        StateEditor as3e = as2.edit(new PatternInterlineDwell(tripPattern1, tripPattern2));
        // trip sequence after this is now [tripC -> tripB]
        as3e.setTripTimes(new TripTimes(tripC, Arrays.asList(stopTime1), new Deduplicator()));
        State as3 = as3e.makeState();
        assertTrue(tripTimesForTripA.tripOrTripSequenceIsBanned(as3, 0));
    }

    @Test
    public void testStopUpdate() {
        TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);

        updatedTripTimesA.updateArrivalTime(3, 190);
        updatedTripTimesA.updateDepartureTime(3, 190);
        updatedTripTimesA.updateArrivalTime(5, 311);
        updatedTripTimesA.updateDepartureTime(5, 312);

        assertEquals(3 * 60 + 10, updatedTripTimesA.getArrivalTime(3));
        assertEquals(3 * 60 + 10, updatedTripTimesA.getDepartureTime(3));
        assertEquals(5 * 60 + 11, updatedTripTimesA.getArrivalTime(5));
        assertEquals(5 * 60 + 12, updatedTripTimesA.getDepartureTime(5));
    }

    @Test
    public void testPassedUpdate() {
        TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);

        updatedTripTimesA.updateDepartureTime(0, TripTimes.UNAVAILABLE);

        assertEquals(TripTimes.UNAVAILABLE, updatedTripTimesA.getDepartureTime(0));
        assertEquals(60, updatedTripTimesA.getArrivalTime(1));
    }

    @Test
    public void testNonIncreasingUpdate() {
        TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);

        updatedTripTimesA.updateArrivalTime(1, 60);
        updatedTripTimesA.updateDepartureTime(1, 59);

        assertFalse(updatedTripTimesA.timesIncreasing());

        TripTimes updatedTripTimesB = new TripTimes(originalTripTimes);

        updatedTripTimesB.updateDepartureTime(6, 421);
        updatedTripTimesB.updateArrivalTime(7, 420);

        assertFalse(updatedTripTimesB.timesIncreasing());
    }

    @Test
    public void testDelay() {
        TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);
        updatedTripTimesA.updateDepartureDelay(0, 10);
        updatedTripTimesA.updateArrivalDelay(6, 13);

        assertEquals(0 * 60 + 10, updatedTripTimesA.getDepartureTime(0));
        assertEquals(6 * 60 + 13, updatedTripTimesA.getArrivalTime(6));
    }

    @Test
    public void testCancel() {
        TripTimes updatedTripTimesA = new TripTimes(originalTripTimes);
        updatedTripTimesA.cancel();

        for (int i = 0; i < stops.length - 1; i++) {
            assertEquals(originalTripTimes.getDepartureTime(i),
                    updatedTripTimesA.getScheduledDepartureTime(i));
            assertEquals(originalTripTimes.getArrivalTime(i),
                    updatedTripTimesA.getScheduledArrivalTime(i));
            assertEquals(TripTimes.UNAVAILABLE, updatedTripTimesA.getDepartureTime(i));
            assertEquals(TripTimes.UNAVAILABLE, updatedTripTimesA.getArrivalTime(i));
        }
    }

    @Test
    public void testApply() {
        Trip trip = new Trip();
        trip.setId(tripId);

        List<StopTime> stopTimes = new LinkedList<StopTime>();

        StopTime stopTime0 = new StopTime();
        StopTime stopTime1 = new StopTime();
        StopTime stopTime2 = new StopTime();

        Stop stop0 = new Stop();
        Stop stop1 = new Stop();
        Stop stop2 = new Stop();

        stop0.setId(stops[0]);
        stop1.setId(stops[1]);
        stop2.setId(stops[2]);

        stopTime0.setStop(stop0);
        stopTime0.setDepartureTime(0);
        stopTime0.setStopSequence(0);

        stopTime1.setStop(stop1);
        stopTime1.setArrivalTime(30);
        stopTime1.setDepartureTime(60);
        stopTime1.setStopSequence(1);

        stopTime2.setStop(stop2);
        stopTime2.setArrivalTime(90);
        stopTime2.setStopSequence(2);

        stopTimes.add(stopTime0);
        stopTimes.add(stopTime1);
        stopTimes.add(stopTime2);

        TripTimes differingTripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

        TripTimes updatedTripTimesA = new TripTimes(differingTripTimes);

        updatedTripTimesA.updateArrivalTime(1, 89);
        updatedTripTimesA.updateDepartureTime(1, 98);

        assertFalse(updatedTripTimesA.timesIncreasing());
    }

    @Test
    public void testGetRunningTime() {
        for (int i = 0; i < stops.length - 1; i++) {
            assertEquals(60, originalTripTimes.getRunningTime(i));
        }

        TripTimes updatedTripTimes = new TripTimes(originalTripTimes);

        for (int i = 0; i < stops.length - 1; i++) {
            updatedTripTimes.updateDepartureDelay(i, i);
        }

        for (int i = 0; i < stops.length - 1; i++) {
            assertEquals(60 - i, updatedTripTimes.getRunningTime(i));
        }
    }

    @Test
    public void testGetDwellTime() {
        for (int i = 0; i < stops.length; i++) {
            assertEquals(0, originalTripTimes.getDwellTime(i));
        }

        TripTimes updatedTripTimes = new TripTimes(originalTripTimes);

        for (int i = 0; i < stops.length; i++) {
            updatedTripTimes.updateArrivalDelay(i, -i);
        }

        for (int i = 0; i < stops.length; i++) {
            assertEquals(i, updatedTripTimes.getDwellTime(i));
        }
    }

    @Test
    public void testCallAndRideBoardTime() {
        // times: 0, 60, 120

        ServiceDay sd = mock(ServiceDay.class);
        when(sd.secondsSinceMidnight(Matchers.anyLong())).thenCallRealMethod();
        int time;

        // time before interval
        time = originalTripTimes.getCallAndRideBoardTime(1, 50, 20, sd, false, 0);
        assertEquals(60, time);

        // time in interval
        time = originalTripTimes.getCallAndRideBoardTime(1, 70, 20, sd, false, 0);
        assertEquals(70, time);

        // time would overlap end of interval
        time = originalTripTimes.getCallAndRideBoardTime(1, 105, 20, sd, false, 0);
        assertTrue(time < 105);

        // time after end of interval
        time = originalTripTimes.getCallAndRideBoardTime(1, 125, 20, sd, false, 0);
        assertTrue(time < 105);

        // clock time before
        time = originalTripTimes.getCallAndRideBoardTime(1, 50, 20, sd, true, 30);
        assertEquals(60, time);

        // clock time in interval
        time = originalTripTimes.getCallAndRideBoardTime(1, 50, 20, sd, true, 70);
        assertEquals(70, time);

        // clock time after interval
        time = originalTripTimes.getCallAndRideBoardTime(1, 50, 20, sd, true, 130);
        assertTrue(time < 50);

        // clock time would cause overlap
        time = originalTripTimes.getCallAndRideBoardTime(1, 50, 20, sd, true, 105);
        assertTrue(time < 50);
    }

    @Test
    public void testCallAndRideAlightTime() {
        ServiceDay sd = mock(ServiceDay.class);
        when(sd.secondsSinceMidnight(Matchers.anyLong())).thenCallRealMethod();
        int time;

        // time after interval
        time = originalTripTimes.getCallAndRideAlightTime(2, 130, 20, sd, false, 0);
        assertEquals(120, time);

        // time in interval
        time = originalTripTimes.getCallAndRideAlightTime(2, 110, 20, sd, false, 0);
        assertEquals(110, time);

        // time in interval, would cause overlap
        time = originalTripTimes.getCallAndRideAlightTime(2, 65, 20, sd, false, 0);
        assertTrue(time == -1 || time > 65);

        // time after interval
        time = originalTripTimes.getCallAndRideAlightTime(2, 55, 20, sd, false, 0);
        assertTrue(time == -1 || time > 65);

        // clock time after interval
        time = originalTripTimes.getCallAndRideAlightTime(2, 130, 20, sd, true, 130);
        assertEquals(-1, time);

        // clock time before board
        time = originalTripTimes.getCallAndRideAlightTime(2, 110, 20, sd, true, 85);
        assertEquals(110, time);

        // clock time after board
        time = originalTripTimes.getCallAndRideAlightTime(2, 110, 20, sd, true, 100);
        assertEquals(-1, time);
    }
}
