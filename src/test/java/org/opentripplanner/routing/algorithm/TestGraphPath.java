package org.opentripplanner.routing.algorithm;

import java.io.File;
import java.util.List;

import com.google.common.collect.Lists;
import junit.framework.TestCase;

import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.factory.PatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.TestUtils;

import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarServiceData;

public class TestGraphPath extends TestCase {
    
    private Graph graph;

    private AStar aStar = new AStar();

    public void setUp() throws Exception {
        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = new Graph();
        PatternHopFactory hl = new PatternHopFactory(context);
        hl.run(graph);
        graph.putService(
                CalendarServiceData.class,
                createCalendarServiceData(context.getOtpTransitService())
        );
    }

    public void testGraphPathOptimize() throws Exception {

        String feedId = graph.getFeedIds().iterator().next();

        Vertex stop_a = graph.getVertex(feedId + ":A");
        Vertex stop_c = graph.getVertex(feedId + ":C");
        Vertex stop_e = graph.getVertex(feedId + ":E");

        ShortestPathTree spt;
        GraphPath path;

        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 0, 0);
        options.setRoutingContext(graph, stop_a.getLabel(), stop_e.getLabel());
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(stop_e, false); /* do not optimize yet, since we are testing optimization */
        assertNotNull(path);

        // Check that the resulting path visits the stops in the right order.
        List<Vertex> stopvs = Lists.newArrayList();
        for (State state : path.states) {
            if (state.getVertex() instanceof TransitStop) {
                stopvs.add(state.getVertex());
            }
        }
        assertTrue(stopvs.get(0) == stop_a);
        assertTrue(stopvs.get(1) == stop_c);
        assertTrue(stopvs.get(2) == stop_e);

        long bestStart = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 20, 0);
        assertNotSame(bestStart, path.getStartTime());

        path = spt.getPath(stop_e, true); /* optimize */
        assertEquals(bestStart, path.getStartTime());
    }
}
