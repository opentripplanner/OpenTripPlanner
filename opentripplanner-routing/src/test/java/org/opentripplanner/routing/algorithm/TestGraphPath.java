package org.opentripplanner.routing.algorithm;

import java.io.File;
import java.util.GregorianCalendar;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.loader.GTFSPatternHopLoader;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import junit.framework.TestCase;

public class TestGraphPath extends TestCase {
    private Graph graph;

    private GtfsContext context;

    public void setUp() throws Exception {

        context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = new Graph();

        GTFSPatternHopLoader hl = new GTFSPatternHopLoader(graph, context);
        hl.load();

    }

    public void testGraphPathOptimize() throws Exception {

        Vertex stop_a = graph.getVertex("agency_A");
        Vertex stop_e = graph.getVertex("agency_E");

        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        ShortestPathTree spt;
        GraphPath path;

        spt = Dijkstra.getShortestPathTree(graph, stop_a.getLabel(), stop_e.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_e, false); /* do not optimize yet, since we are testing optimization */
        assertNotNull(path);
        assertEquals(11, path.vertices.size());

        long bestStart = new GregorianCalendar(2009, 8, 7, 0, 20, 0).getTimeInMillis();
        assertNotSame(bestStart, path.vertices.firstElement().state.getTime());
        path.optimize();
        assertEquals(bestStart, path.vertices.firstElement().state.getTime());
    }
}
