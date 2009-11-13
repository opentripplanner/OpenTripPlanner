package org.opentripplanner.routing.edgetype.factory;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.loader.GTFSHopLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestHopFactory extends TestCase {

    public void testBasic() throws Exception {

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));
        Graph graph = new Graph();
        GTFSHopLoader loader = new GTFSHopLoader(graph, context);
        loader.load(null);
        Set<Hop> hopSet = new HashSet<Hop>();
        for (Vertex v : graph.getVertices()) {
            for (Edge e : v.getOutgoing()) {
                if (e instanceof Hop) {
                    hopSet.add((Hop) e);
                }
            }
        }

        ArrayList<Hop> hops = new ArrayList<Hop>(hopSet);

        Collections.sort(hops, new Hop.HopArrivalTimeComparator());
        Hop last = hops.get(hops.size() - 1);
        assertEquals(91740, last.getStartStopTime().getDepartureTime());
    }
}
