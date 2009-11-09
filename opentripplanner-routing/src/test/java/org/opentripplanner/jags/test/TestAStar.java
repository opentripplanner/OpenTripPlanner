package org.opentripplanner.jags.test;

import java.io.File;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.jags.algorithm.AStar;
import org.opentripplanner.jags.algorithm.Dijkstra;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TraverseOptions;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.loader.GTFSPatternHopLoader;
import org.opentripplanner.jags.gtfs.GtfsContext;
import org.opentripplanner.jags.gtfs.GtfsLibrary;
import org.opentripplanner.jags.spt.GraphPath;
import org.opentripplanner.jags.spt.ShortestPathTree;

public class TestAStar extends TestCase {
    public void testBasic() throws Exception {

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));

        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        Graph gg = new Graph();
        GTFSPatternHopLoader hl = new GTFSPatternHopLoader(gg, context);
        hl.load();

        long startTime = new GregorianCalendar(2009, 8, 7, 12, 0, 0).getTimeInMillis();
        ShortestPathTree spt = AStar.getShortestPathTree(gg, "Caltrain_Millbrae Caltrain",
                "Caltrain_Mountain View Caltrain", new State(startTime), options);

        GraphPath path = spt.getPath(gg.getVertex("Caltrain_Mountain View Caltrain"));

        GregorianCalendar endTime = new GregorianCalendar(2009, 8, 7, 13, 29);

        assertEquals(path.vertices.lastElement().state.getTime(), endTime.getTimeInMillis());
    }

    public void testCompareSpeed() throws Exception {

        Graph graph;

        GtfsContext context;

        graph = ConstantsForTests.getInstance().getPortlandGraph();
        context = ConstantsForTests.getInstance().getPortlandContext();

        Vertex airport = graph.getVertex("TriMet_10579");
        TraverseOptions wo = new TraverseOptions();
        wo.setGtfsContext(context);
        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 1, 12, 34, 25);

        long startClock, endClock;
        ShortestPathTree spt;

        /* time Dijkstra */
         startClock = System.nanoTime();
         
         spt = Dijkstra.getShortestPathTree(graph, "TriMet_6876", airport.label, new
         State(startTime.getTimeInMillis()), wo);
         
        endClock = System.nanoTime();
        long aStarSpeed = endClock - startClock;

        GraphPath path = spt.getPath(airport);
        assertNotNull(path);
         
        /* time A* */
        startClock = System.nanoTime();

        spt = AStar.getShortestPathTree(graph, "TriMet_6876", airport.label, new State(startTime
                .getTimeInMillis()), wo);

        endClock = System.nanoTime();
        long dijkstraSpeed = endClock - startClock;

        assertTrue(aStarSpeed >= dijkstraSpeed);

        path = spt.getPath(airport);


    }
}
