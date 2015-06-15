package org.opentripplanner.analyst.scenario;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKTReader;
import gnu.trove.map.TIntIntMap;
import junit.framework.TestCase;
import org.junit.Test;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.profile.RaptorWorkerData;
import org.opentripplanner.profile.TimeWindow;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.time.DayOfWeek;
import java.util.BitSet;

import static org.opentripplanner.graph_builder.module.FakeGraph.*;

/**
 * Test adding trip patterns.
 */
public class AddTripPatternTest extends TestCase {
    /** Make sure that stops are properly linked into the graph */
    @Test
    public void testStopLinking () throws Exception {
        AddTripPattern atp = getAddTripPattern();
        atp.timetables.add(getTimetable(true));

        // get a graph
        Graph g = buildGraphNoTransit();
        addRegularStopGrid(g);
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

        RaptorWorkerData data = new RaptorWorkerData(g, window, scenario);

        assertEquals(5, data.nStops);

        // make sure we can find the stops
        AStar aStar = new AStar();
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.from = new GenericLocation(39.963417, -82.980799);
        rr.batch = true;
        rr.setRoutingContext(g);
        rr.batch = true;

        ShortestPathTree spt = aStar.getShortestPathTree(rr);

        TIntIntMap stops = data.findStopsNear(spt, g);

        // we should have found stops
        assertFalse(stops.isEmpty());

        // ensure that the times made it into the data
        // This assumes worst-case departure, and the first worst departure is 10:30 after the service
        // starts running (dwell + headway)
        assertEquals(4 * 3600 + 600 + 30, data.timetablesForPattern.get(0).getFrequencyDeparture(0, 0, 39 * 360, true));
    }
    
    private AddTripPattern getAddTripPattern () throws Exception {
        AddTripPattern atp = new AddTripPattern();
        WKTReader wr = new WKTReader();
        // Running west on Broad Street from Bexley to High, and then north to the Short North,
        // in Columbus, OH
        atp.geometry = (LineString)
                wr.read("LINESTRING(-82.93727602046642744 39.96934234865877045, -82.98058356730228979 39.96435660398918088, -82.99808255349556418 39.96259692939991481, -83.00091758477827852 39.96357452639394836, -83.00492573245382744 39.98283318717648882, -83.00639212794487776 39.99065396312879273)");

        atp.name = "Broad High Express";
        atp.stops = new BitSet();

        // everything is a stop except for (0-based) point 3, on High one block north of Broad
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
}
