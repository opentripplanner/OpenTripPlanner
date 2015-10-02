package org.opentripplanner.analyst.scenario;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKTReader;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TIntIntMap;
import junit.framework.TestCase;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.profile.*;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.BitSet;

import static org.opentripplanner.graph_builder.module.FakeGraph.*;

/**
 * Test adding trip patterns.
 */
public class AddTripPatternTest extends TestCase {
    /** Make sure that stops are properly linked into the graph */
    @Test
    public void testStopLinking () throws Exception {
        AddTripPattern atp = getAddTripPattern(RouteSelector.BROAD_HIGH);
        atp.timetables.add(getTimetable(true));

        // get a graph
        Graph g = buildGraphNoTransit();
        link(g);
        g.index(new DefaultStreetVertexIndexFactory());

        // materialize the trip pattern
        atp.materialize(g);

        // there should be five stops because one point is not a stop
        assertEquals(5, atp.temporaryStops.length);

        // they should all be linked into the graph
        for (int i = 0; i < atp.temporaryStops.length; i++) {
            assertNotNull(atp.temporaryStops[i].sample);
            assertNotNull(atp.temporaryStops[i].sample.v0);
            assertNotNull(atp.temporaryStops[i].sample.v1);
        }


        // no services running: not needed for trips added on the fly.
        TimeWindow window = new TimeWindow(7 * 3600, 9 * 3600, new BitSet(), DayOfWeek.WEDNESDAY);

        Scenario scenario = new Scenario(0);
        scenario.modifications = Lists.newArrayList(atp);
        ProfileRequest req = new ProfileRequest();
        req.scenario = scenario;
        req.boardingAssumption = RaptorWorkerTimetable.BoardingAssumption.WORST_CASE;

        RaptorWorkerData data = new RaptorWorkerData(g, window, req);
        assertEquals(5, data.nStops);

        // make sure we can find the stops
        AStar aStar = new AStar();
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.from = new GenericLocation(39.963417, -82.980799);
        rr.batch = true;
        rr.setRoutingContext(g);
        rr.batch = true;

        ShortestPathTree spt = aStar.getShortestPathTree(rr);

        TIntIntMap stops = data.findStopsNear(spt, g, false, 1.3f);

        // we should have found stops
        assertFalse(stops.isEmpty());

        // ensure that the times made it into the data
        // This assumes worst-case departure, and the first worst departure is 10:30 after the service
        // starts running (dwell + headway)
        assertEquals(4 * 3600 + 600 + 30,
                data.timetablesForPattern.get(0).getFrequencyDeparture(0, 0, 39 * 360,
                        -1, null));
    }

    /** Test adding trips with a timetable rather than frequencies */
    @Test
    public void testTimetableTrips () throws Exception {
        AddTripPattern atp = getAddTripPattern(RouteSelector.BROAD_HIGH);
        atp.timetables.add(getTimetable(false));

        // get a graph
        Graph g = buildGraphNoTransit();
        link(g);
        g.index(new DefaultStreetVertexIndexFactory());

        // materialize the trip pattern
        atp.materialize(g);

        // there should be five stops because one point is not a stop
        assertEquals(5, atp.temporaryStops.length);

        // they should all be linked into the graph
        for (int i = 0; i < atp.temporaryStops.length; i++) {
            assertNotNull(atp.temporaryStops[i].sample);
            assertNotNull(atp.temporaryStops[i].sample.v0);
            assertNotNull(atp.temporaryStops[i].sample.v1);
        }


        // no services running: not needed for trips added on the fly.
        TimeWindow window = new TimeWindow(7 * 3600, 9 * 3600, new BitSet(), DayOfWeek.WEDNESDAY);

        Scenario scenario = new Scenario(0);
        scenario.modifications = Lists.newArrayList(atp);
        ProfileRequest req = new ProfileRequest();
        req.scenario = scenario;
        req.boardingAssumption = RaptorWorkerTimetable.BoardingAssumption.WORST_CASE;

        RaptorWorkerData data = new RaptorWorkerData(g, window, req);

        assertEquals(5, data.nStops);

        // make sure we can find the stops
        AStar aStar = new AStar();
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.from = new GenericLocation(39.963417, -82.980799);
        rr.batch = true;
        rr.setRoutingContext(g);
        rr.batch = true;

        ShortestPathTree spt = aStar.getShortestPathTree(rr);

        TIntIntMap stops = data.findStopsNear(spt, g, false, 1.3f);

        // we should have found stops
        assertFalse(stops.isEmpty());

        // ensure that the times made it into the data
        // This is after the first dwell time has been applied
        assertEquals(7 * 3600 + 30, data.timetablesForPattern.get(0).getDeparture(0, 0));
    }

    /** Make sure that transfers work */
    @Test
    public void testTransfers () throws Exception {
        AddTripPattern atp = getAddTripPattern(RouteSelector.BROAD_HIGH);
        atp.timetables.add(getTimetable(false));

        AddTripPattern atp2 = getAddTripPattern(RouteSelector.BEXLEY_CMH);
        atp2.timetables.add(getTimetable(true));

        // get a graph
        Graph g = buildGraphNoTransit();
        addTransit(g);
        link(g);
        g.index(new DefaultStreetVertexIndexFactory());

        // materialize the trip pattern
        atp.materialize(g);
        atp2.materialize(g);

        TimeWindow window = new TimeWindow(7 * 3600, 9 * 3600, g.index.servicesRunning(new LocalDate(2015, 6, 10)), DayOfWeek.WEDNESDAY);

        Scenario scenario = new Scenario(0);
        scenario.modifications = Lists.newArrayList(atp, atp2);
        ProfileRequest req = new ProfileRequest();
        req.scenario = scenario;
        req.boardingAssumption = RaptorWorkerTimetable.BoardingAssumption.WORST_CASE;

        RaptorWorkerData data = new RaptorWorkerData(g, window, req);

        // make sure that we have transfers a) between the new lines b) from the new lines
        // to the existing lines c) from the existing lines to the new lines
        // stop IDs in the data will be 0 and 1 for existing stops, 2 - 6 for Broad/High and 7 - 11 for Bexley/CMH
        int[] txFromExisting = data.transfersForStop.get(0);
        if (txFromExisting.length == 0)
            txFromExisting = data.transfersForStop.get(1);

        // make sure there's a transfer to stop 4 (Broad/High)
        // the AddTripPattern instructions are processed in order
        // also recall that each transfer has two ints in the array as it's a jagged array of
        // dest_pattern, distance
        assertTrue(txFromExisting.length > 0);

        boolean foundTx = false;

        for (int i = 0; i < txFromExisting.length; i += 2) {
            if (txFromExisting[i] == 4) {
                foundTx = true;
                break;
            }
        }

        assertTrue("transfer from existing to new", foundTx);

        // Check that there are transfers from the new route to the existing route
        // This is the stop at Broad and High
        int[] txToExisting = data.transfersForStop.get(4);
        assertTrue(txToExisting.length > 0);
        foundTx = false;

        for (int i = 0; i < txToExisting.length; i += 2) {
            if (txToExisting[i] == 0 || txToExisting[i] == 1) {
                // stop from existing route
                foundTx = true;
                break;
            }
        }

        assertTrue("transfer from new to existing", foundTx);

        // Check that there are transfers between the new routes
        int[] txBetweenNew = data.transfersForStop.get(7);
        assertTrue(txBetweenNew.length > 0);
        foundTx = false;

        for (int i = 0; i < txBetweenNew.length; i += 2) {
            if (txBetweenNew[i] == 2) {
                foundTx = true;
                break;
            }
        }

        assertTrue(foundTx);
    }

    /** Test the full routing */
    @Test
    public void integrationTest () throws Exception {
        Graph g = buildGraphNoTransit();
        addTransit(g);
        link(g);

        ProfileRequest pr1 = new ProfileRequest();
        pr1.date = new LocalDate(2015, 6, 10);
        pr1.fromTime = 7 * 3600;
        pr1.toTime = 9 * 3600;
        pr1.fromLat = pr1.toLat = 39.9621;
        pr1.fromLon = pr1.toLon = -83.0007;

        RepeatedRaptorProfileRouter rrpr1 = new RepeatedRaptorProfileRouter(g, pr1);
        rrpr1.route();

        ProfileRequest pr2 = new ProfileRequest();
        pr2.date = new LocalDate(2015, 6, 10);
        pr2.fromTime = 7 * 3600;
        pr2.toTime = 9 * 3600;
        pr2.fromLat = pr2.toLat = 39.9621;
        pr2.fromLon = pr2.toLon = -83.0007;

        AddTripPattern atp = getAddTripPattern(RouteSelector.BROAD_HIGH);
        atp.timetables.add(getTimetable(true));

        pr2.scenario = new Scenario(0);
        pr2.scenario.modifications = Arrays.asList(atp);

        RepeatedRaptorProfileRouter rrpr2 = new RepeatedRaptorProfileRouter(g, pr2);
        rrpr2.route();

        boolean foundDecrease = false;

        // make sure that travel time did not increase
        for (TObjectIntIterator<Vertex> vit = rrpr1.timeSurfaceRangeSet.min.times.iterator(); vit.hasNext();) {
            vit.advance();

            int time2 = rrpr2.timeSurfaceRangeSet.min.getTime(vit.key());

            assertTrue(time2 <= vit.value());

            if (time2 < vit.value()) foundDecrease = true;
        }

        assertTrue("found decreases in travel time due to adding route", foundDecrease);
    }

    private AddTripPattern getAddTripPattern (RouteSelector sel) throws Exception {
        AddTripPattern atp = new AddTripPattern();
        WKTReader wr = new WKTReader();
        atp.geometry = sel.getGeometry();

        atp.name = "Broad High Express";
        atp.stops = new BitSet();

        // everything is a stop except for (0-based) point 3, on High one block north of Broad
        // or on East Broad, depending on geometry chosen
        for (int i = 0; i < 6; i++) {
            if (i == 3)
                atp.stops.clear(i);
            else
                atp.stops.set(i);
        }

        atp.timetables = Lists.newArrayList();

        return atp;
    }

    /**
     * Get a timetable. If frequency = true, run every 10 minutes from 4AM to 10PM local.
     * If frequency = false, one run at 7 AM.
     */
    private AddTripPattern.PatternTimetable getTimetable (boolean frequency) {
        AddTripPattern.PatternTimetable tt = new AddTripPattern.PatternTimetable();
        tt.days = new BitSet();
        // wednesday service only
        tt.days.set(2);

        tt.dwellTimes = new int[] { 30, 30, 30, 30, 30 };
        tt.hopTimes = new int[] { 90, 90, 90, 90 };

        tt.frequency = frequency;

        if (frequency) {
            tt.startTime = 4 * 3600;
            tt.endTime = 22 * 3600;
            tt.headwaySecs = 600;
        }
        else
            tt.startTime = 7 * 3600;

        return tt;
    }

    /** Just a switch between two different route geometries */
    private static enum RouteSelector {
        BROAD_HIGH, BEXLEY_CMH;

        public LineString getGeometry () throws Exception {
            WKTReader wr = new WKTReader();
            switch (this) {
            // Running west on Broad Street from Bexley to High, and then north to the Short North,
            // in Columbus, OH
            case BROAD_HIGH:
                return (LineString) wr.read("LINESTRING(-82.93727602046642744 39.96934234865877045, -82.98058356730228979 39.96435660398918088, -82.99808255349556418 39.96259692939991481, -83.00091758477827852 39.96357452639394836, -83.00492573245382744 39.98283318717648882, -83.00639212794487776 39.99065396312879273)");
            // Running east on Broad Street and then up Hamilton to Sawyer and the CMH airport
            case BEXLEY_CMH:
                return (LineString) wr.read("LINESTRING(-82.93296696 39.96964327, -82.91302398 39.97218502, -82.88408711 39.97492229, -82.8739201 39.97570437, -82.86864107 39.99603838, -82.89444963 39.99897118)");
            default:
                // can't happen
                return null;
            }
        }
    }
}
