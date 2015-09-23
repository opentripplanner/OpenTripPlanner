package org.opentripplanner.profile;

import junit.framework.TestCase;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.opentripplanner.analyst.scenario.ConvertToFrequency;
import org.opentripplanner.analyst.scenario.Scenario;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.profile.RaptorWorkerTimetable;
import org.opentripplanner.profile.RepeatedRaptorProfileRouter;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;

import java.util.Arrays;

import static org.opentripplanner.graph_builder.module.FakeGraph.*;

/**
 * Make sure that converting lines to frequency-based representations works.
 */
public class ConvertToFrequencyTest extends TestCase {
    /** The simplest case of frequency conversion: no weird loop routes or anything like that, travel times always same, etc. */
    @Test
    public void testSimpleConversion () throws Exception {
        Graph gg = buildGraphNoTransit();
        addTransit(gg);
        link(gg);
        gg.index(new DefaultStreetVertexIndexFactory());

        ProfileRequest pr1 = new ProfileRequest();
        pr1.date = new LocalDate(2015, 6, 10);
        pr1.fromTime = 7 * 3600;
        pr1.toTime = 9 * 3600;
        pr1.fromLat = pr1.toLat = 39.9621;
        pr1.fromLon = pr1.toLon = -83.0007;
        pr1.accessModes = pr1.egressModes = pr1.directModes = new QualifiedModeSet("WALK");
        pr1.transitModes = new TraverseModeSet("TRANSIT");

        RepeatedRaptorProfileRouter rrpr1 = new RepeatedRaptorProfileRouter(gg, pr1);
        rrpr1.route();

        ProfileRequest pr2 = new ProfileRequest();
        pr2.date = new LocalDate(2015, 6, 10);
        pr2.fromTime = 7 * 3600;
        pr2.toTime = 9 * 3600;
        pr2.fromLat = pr2.toLat = 39.9621;
        pr2.fromLon = pr2.toLon = -83.0007;
        pr2.accessModes = pr2.egressModes = pr2.directModes = new QualifiedModeSet("WALK");
        pr2.transitModes = new TraverseModeSet("TRANSIT");

        ConvertToFrequency ctf  = new ConvertToFrequency();
        ctf.groupBy = ConvertToFrequency.ConversionGroup.ROUTE_DIRECTION;
        ctf.routeId = new String [] { "route" };
        ctf.windowStart = 5 * 3600;
        ctf.windowEnd = 10 * 3600;

        pr2.scenario = new Scenario(0);
        pr2.scenario.modifications = Arrays.asList(ctf);

        RepeatedRaptorProfileRouter rrpr2 = new RepeatedRaptorProfileRouter(gg, pr2);
        rrpr2.route();

        assertFalse(rrpr1.raptorWorkerData.hasFrequencies);
        assertTrue(rrpr2.raptorWorkerData.hasFrequencies);

        RaptorWorkerTimetable tt = rrpr2.raptorWorkerData.timetablesForPattern.get(0);
        assertEquals(FakeGraph.FREQUENCY, tt.headwaySecs[0]);
        assertEquals(FakeGraph.TRAVEL_TIME, tt.frequencyTrips[0][2]);
    }

    /**
     * Test the case where there are multiple patterns that need to be chosen from.
     */
    @Test
    public void testMultiplePatterns () throws Exception {
        Graph gg = buildGraphNoTransit();
        addMultiplePatterns(gg);
        link(gg);
        gg.index(new DefaultStreetVertexIndexFactory());

        ProfileRequest pr1 = new ProfileRequest();
        pr1.date = new LocalDate(2015, 6, 10);
        pr1.fromTime = 7 * 3600;
        pr1.toTime = 9 * 3600;
        pr1.fromLat = pr1.toLat = 39.9621;
        pr1.fromLon = pr1.toLon = -83.0007;
        pr1.accessModes = pr1.egressModes = pr1.directModes = new QualifiedModeSet("WALK");
        pr1.transitModes = new TraverseModeSet("TRANSIT");

        RepeatedRaptorProfileRouter rrpr1 = new RepeatedRaptorProfileRouter(gg, pr1);
        rrpr1.route();

        ProfileRequest pr2 = new ProfileRequest();
        pr2.date = new LocalDate(2015, 6, 10);
        pr2.fromTime = 7 * 3600;
        pr2.toTime = 9 * 3600;
        pr2.fromLat = pr2.toLat = 39.9621;
        pr2.fromLon = pr2.toLon = -83.0007;
        pr2.accessModes = pr2.egressModes = pr2.directModes = new QualifiedModeSet("WALK");
        pr2.transitModes = new TraverseModeSet("TRANSIT");

        ConvertToFrequency ctf  = new ConvertToFrequency();
        ctf.groupBy = ConvertToFrequency.ConversionGroup.ROUTE_DIRECTION;
        ctf.routeId = new String [] { "route" };
        ctf.windowStart = 5 * 3600;
        ctf.windowEnd = 10 * 3600;

        pr2.scenario = new Scenario(0);
        pr2.scenario.modifications = Arrays.asList(ctf);

        RepeatedRaptorProfileRouter rrpr2 = new RepeatedRaptorProfileRouter(gg, pr2);
        rrpr2.route();

        assertFalse(rrpr1.raptorWorkerData.hasFrequencies);
        assertTrue(rrpr2.raptorWorkerData.hasFrequencies);

        // everything should have gotten merged into one pattern
        assertEquals(1, rrpr2.raptorWorkerData.timetablesForPattern.size());
        RaptorWorkerTimetable tt = rrpr2.raptorWorkerData.timetablesForPattern.get(0);

        // should be no frequency variation because trips on all patterns are considered for frequencies.
        // there should be no travel time variation because only trips on the dominant pattern are considered
        // for travel time.
        assertEquals(FakeGraph.FREQUENCY, tt.headwaySecs[0]);
        assertEquals(FakeGraph.TRAVEL_TIME, tt.frequencyTrips[0][2]);

        // now try it with groupings by pattern
        ConvertToFrequency ctf3  = new ConvertToFrequency();
        ctf3.groupBy = ConvertToFrequency.ConversionGroup.PATTERN;
        ctf3.routeId = new String [] { "route" };
        ctf3.windowStart = 5 * 3600;
        ctf3.windowEnd = 10 * 3600;

        ProfileRequest pr3 = new ProfileRequest();
        pr3.date = new LocalDate(2015, 6, 10);
        pr3.fromTime = 7 * 3600;
        pr3.toTime = 9 * 3600;
        pr3.fromLat = pr2.toLat = 39.9621;
        pr3.fromLon = pr2.toLon = -83.0007;
        pr3.accessModes = pr2.egressModes = pr2.directModes = new QualifiedModeSet("WALK");
        pr3.transitModes = new TraverseModeSet("TRANSIT");
        pr3.scenario = new Scenario(0);
        pr3.scenario.modifications = Arrays.asList(ctf3);

        RepeatedRaptorProfileRouter rrpr3 = new RepeatedRaptorProfileRouter(gg, pr3);
        rrpr3.route();

        assertTrue(rrpr3.raptorWorkerData.hasFrequencies);
        // should be converted to two independent patterns
        assertEquals(2, rrpr3.raptorWorkerData.timetablesForPattern.size());

        RaptorWorkerTimetable shrt, lng;
        if (rrpr3.raptorWorkerData.timetablesForPattern.get(0).nStops == 2) {
            shrt = rrpr3.raptorWorkerData.timetablesForPattern.get(0);
            lng = rrpr3.raptorWorkerData.timetablesForPattern.get(1);
        } else {
            lng = rrpr3.raptorWorkerData.timetablesForPattern.get(0);
            shrt = rrpr3.raptorWorkerData.timetablesForPattern.get(1);
        }

        assertEquals(3, lng.nStops);
        assertEquals(2, shrt.nStops);

        assertEquals(675, lng.headwaySecs[0]);
        assertEquals((int) (FakeGraph.FREQUENCY / 0.1), shrt.headwaySecs[0]);

        // make sure that the hop time is always FakeGraph.TRAVEL_TIME
        assertEquals(FakeGraph.TRAVEL_TIME, shrt.frequencyTrips[0][2]);
        assertEquals(FakeGraph.TRAVEL_TIME, lng.frequencyTrips[0][2]);
        assertEquals(FakeGraph.TRAVEL_TIME * 2, lng.frequencyTrips[0][4]);
    }

    /** Test bidirectional conversion */
    @Test
    public void testBidirectional () throws Exception {
        Graph gg = buildGraphNoTransit();
        addTransitBidirectional(gg);
        link(gg);
        gg.index(new DefaultStreetVertexIndexFactory());

        ProfileRequest pr2 = new ProfileRequest();
        pr2.date = new LocalDate(2015, 6, 10);
        pr2.fromTime = 7 * 3600;
        pr2.toTime = 9 * 3600;
        pr2.fromLat = pr2.toLat = 39.9621;
        pr2.fromLon = pr2.toLon = -83.0007;
        pr2.accessModes = pr2.egressModes = pr2.directModes = new QualifiedModeSet("WALK");
        pr2.transitModes = new TraverseModeSet("TRANSIT");

        ConvertToFrequency ctf  = new ConvertToFrequency();
        ctf.groupBy = ConvertToFrequency.ConversionGroup.ROUTE_DIRECTION;
        ctf.routeId = new String [] { "route" };
        ctf.windowStart = 5 * 3600;
        ctf.windowEnd = 10 * 3600;

        pr2.scenario = new Scenario(0);
        pr2.scenario.modifications = Arrays.asList(ctf);

        RepeatedRaptorProfileRouter rrpr2 = new RepeatedRaptorProfileRouter(gg, pr2);
        rrpr2.route();

        assertTrue(rrpr2.raptorWorkerData.hasFrequencies);

        assertEquals(2, rrpr2.raptorWorkerData.timetablesForPattern.size());

        // make sure we got trips in both directions
        RaptorWorkerTimetable tt = rrpr2.raptorWorkerData.timetablesForPattern.get(0);
        RaptorWorkerTimetable tt2 = rrpr2.raptorWorkerData.timetablesForPattern.get(1);

        assertEquals(2, tt2.stopIndices.length);
        assertEquals(2, tt.stopIndices.length);

        assertEquals(tt.stopIndices[0], tt2.stopIndices[1]);
        assertEquals(tt.stopIndices[1], tt2.stopIndices[0]);
    }
}