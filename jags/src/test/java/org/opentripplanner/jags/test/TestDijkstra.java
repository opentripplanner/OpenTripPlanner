package org.opentripplanner.jags.test;

import java.io.File;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.jags.algorithm.Dijkstra;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.edgetype.loader.GTFSPatternHopLoader;
import org.opentripplanner.jags.gtfs.GtfsContext;
import org.opentripplanner.jags.gtfs.GtfsLibrary;
import org.opentripplanner.jags.spt.GraphPath;
import org.opentripplanner.jags.spt.ShortestPathTree;

public class TestDijkstra extends TestCase {
    public void testBasic() throws Exception {

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));

        WalkOptions options = new WalkOptions();
        options.setGtfsContext(context);

        Graph gg = new Graph();
        GTFSPatternHopLoader hl = new GTFSPatternHopLoader(gg, context);
        hl.load();

        long startTime = new GregorianCalendar(2009, 8, 7, 12, 0, 0).getTimeInMillis();
        ShortestPathTree spt = Dijkstra.getShortestPathTree(gg, "Caltrain_Millbrae Caltrain",
                "Caltrain_Mountain View Caltrain", new State(startTime), options);

        GraphPath path = spt.getPath(gg.getVertex("Caltrain_Mountain View Caltrain"));

        GregorianCalendar endTime = new GregorianCalendar(2009, 8, 7, 13, 29);
        
        assertEquals(path.vertices.lastElement().state.getTime(), endTime.getTimeInMillis());
    }
}
