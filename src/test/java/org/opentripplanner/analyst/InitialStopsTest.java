package org.opentripplanner.analyst;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import junit.framework.TestCase;
import junit.framework.TestResult;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.opentripplanner.analyst.cluster.TaskStatistics;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.profile.RaptorWorkerData;
import org.opentripplanner.profile.RepeatedRaptorProfileRouter;
import org.opentripplanner.profile.TimeWindow;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;

import static org.opentripplanner.graph_builder.module.FakeGraph.*;

/**
 * Test the code that finds initial transit stops.
 */
public class InitialStopsTest extends TestCase {
    /** Time (in seconds) that a decrease must be greater than in order to be considered a decrease (make sure tests don't inadvertently pass due to rounding errors) */
    public static final int EPSILON = 10;

    /**
     * Test that increasing the bike speed on a bike-to-transit search
     * a) decreases or leaves unchanged all access times.
     * b) allows access to a superset of the originally accessible stops.
     *
     * There was once a bug where bike speed was not correctly applied because we used the distance not the speed.
     */
    @Test
    public void testInitialStopBikeSpeedIncrease () throws Exception {
        Graph g = buildGraphNoTransit();
        addRegularStopGrid(g);
        addTransitMultipleLines(g);
        link(g);
        g.index(new DefaultStreetVertexIndexFactory());

        ProfileRequest req = new ProfileRequest();
        req.fromLon = req.toLon = -83.0118;
        req.fromLat = req.toLat = 39.9908;
        req.date = new LocalDate(2015, 9, 17);
        req.bikeSpeed = 4.1f;
        req.walkSpeed = 1.3f;
        req.fromTime = 7 * 3600;
        req.toTime = 9 * 3600;
        req.maxBikeTime = 20;
        req.transitModes = new TraverseModeSet("TRANSIT");
        req.accessModes = req.egressModes = req.directModes = new QualifiedModeSet("BICYCLE");

        RaptorWorkerData data = RepeatedRaptorProfileRouter.getRaptorWorkerData(req, g, null, new TaskStatistics());
        assertNotNull(data);
        RepeatedRaptorProfileRouter rrpr = new RepeatedRaptorProfileRouter(g, req);
        TIntIntMap initialStops1 = rrpr.findInitialStops(false, data);

        assertFalse(initialStops1.isEmpty());

        // let's get crazy, set bike speed really high.
        req.bikeSpeed = 25f;
        data = RepeatedRaptorProfileRouter.getRaptorWorkerData(req, g, null, new TaskStatistics());
        assertNotNull(data);
        rrpr = new RepeatedRaptorProfileRouter(g, req);
        TIntIntMap initialStops2 = rrpr.findInitialStops(false, data);

        // we should find decreases to at least some stops
        boolean foundDecreases = false;

        for (TIntIntIterator it = initialStops1.iterator(); it.hasNext();) {
            it.advance();

            // the reached stops from the faster search should be a superset of the reached stops from the slower search
            assertTrue(initialStops2.containsKey(it.key()));

            assertTrue("Found increase in travel time to stop", initialStops2.get(it.key()) <= it.value());

            foundDecreases = foundDecreases || initialStops2.get(it.key()) < it.value() - EPSILON;
        }

        assertTrue(foundDecreases);
    }

    /**
     * Test that increasing the walk speed on a walk-to-transit search
     * a) decreases or leaves unchanged all access times.
     * b) allows access to a superset of the originally accessible stops.
     * c) decreases at least some access times.
     *
     * There was once a bug where bike speed was not correctly applied because we used the distance not the speed.
     */
    @Test
    public void testInitialStopWalkSpeedIncrease () throws Exception {
        Graph g = buildGraphNoTransit();
        addRegularStopGrid(g);
        addTransitMultipleLines(g);
        link(g);
        g.index(new DefaultStreetVertexIndexFactory());

        ProfileRequest req = new ProfileRequest();
        req.fromLon = req.toLon = -83.0118;
        req.fromLat = req.toLat = 39.9908;
        req.date = new LocalDate(2015, 9, 17);
        req.bikeSpeed = 4.1f;
        req.walkSpeed = 1.3f;
        req.fromTime = 7 * 3600;
        req.toTime = 9 * 3600;
        req.maxBikeTime = 20;
        req.maxWalkTime = 20;
        req.transitModes = new TraverseModeSet("TRANSIT");
        req.accessModes = req.egressModes = req.directModes = new QualifiedModeSet("WALK");

        RaptorWorkerData data = RepeatedRaptorProfileRouter.getRaptorWorkerData(req, g, null, new TaskStatistics());
        assertNotNull(data);
        RepeatedRaptorProfileRouter rrpr = new RepeatedRaptorProfileRouter(g, req);
        TIntIntMap initialStops1 = rrpr.findInitialStops(false, data);

        assertFalse(initialStops1.isEmpty());

        // let's get crazy, set walk speed really high.
        req.walkSpeed = 25f;
        data = RepeatedRaptorProfileRouter.getRaptorWorkerData(req, g, null, new TaskStatistics());
        assertNotNull(data);
        rrpr = new RepeatedRaptorProfileRouter(g, req);
        TIntIntMap initialStops2 = rrpr.findInitialStops(false, data);

        // we should find decreases to at least some stops
        boolean foundDecreases = false;

        for (TIntIntIterator it = initialStops1.iterator(); it.hasNext();) {
            it.advance();

            // the reached stops from the faster search should be a superset of the reached stops from the slower search
            assertTrue(initialStops2.containsKey(it.key()));

            assertTrue("Found increase in travel time to stop", initialStops2.get(it.key()) <= it.value());

            foundDecreases = foundDecreases || initialStops2.get(it.key()) < it.value() - EPSILON;
        }

        assertTrue("No decreases were found due to increased walk speed", foundDecreases);
    }
}
