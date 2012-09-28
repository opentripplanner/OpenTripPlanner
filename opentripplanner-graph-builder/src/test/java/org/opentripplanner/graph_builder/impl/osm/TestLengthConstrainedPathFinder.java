package org.opentripplanner.graph_builder.impl.osm;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;
import org.opentripplanner.graph_builder.impl.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.impl.osm.OpenStreetMapGraphBuilderImpl;
import org.opentripplanner.openstreetmap.impl.FileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.util.LengthConstrainedPathFinder;
import org.opentripplanner.routing.util.LengthConstrainedPathFinder.State;

import junit.framework.TestCase;

public class TestLengthConstrainedPathFinder extends TestCase {

    private HashMap<Class<?>, Object> extra = new HashMap<Class<?>, Object>();

    private static final boolean DEBUG_OUTPUT = false;

    @Test
    public void testFinder() throws Exception {

        Graph gg = new Graph();

        OpenStreetMapGraphBuilderImpl loader = new OpenStreetMapGraphBuilderImpl();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        FileBasedOpenStreetMapProviderImpl provider = new FileBasedOpenStreetMapProviderImpl();
        File file = new File(getClass().getResource("map.osm.gz").getFile());
        provider.setPath(file);
        loader.setProvider(provider);
        loader.buildGraph(gg, extra);
        if (DEBUG_OUTPUT)
            gg.save(new File("/home/abyrd/constrain.graph"));

        Vertex v1 = gg.getVertex("osm node 300879101");
        Vertex v2 = gg.getVertex("osm node 300879072");
        Vertex v3 = gg.getVertex("osm node 300879136");
        Vertex v4 = gg.getVertex("osm node 300879035");

        Edge e1 = null, e2 = null;
        for (Edge e : v1.getOutgoing()) {
            if (e.getToVertex() == v2) {
                e1 = e;
            }
        }
        for (Edge e : v3.getOutgoing()) {
            if (e.getToVertex() == v4) {
                e2 = e;
            }
        }

        assertNotNull(e1);
        assertNotNull(e2);

        LengthConstrainedPathFinder finder;
        Set<State> solutions;

        for (boolean prune : new boolean[] { false, true }) {
            System.out.printf("%s\n", prune);
            long elapsed = 0;
            for (int pathLength = 100; elapsed < 2000 && pathLength < 1000; pathLength += 50) {
                long t0 = System.currentTimeMillis();
                finder = new LengthConstrainedPathFinder(e1, e2, pathLength, 0, prune);
                solutions = finder.solveDepthFirst();
                long t1 = System.currentTimeMillis();
                elapsed = t1 - t0;
                System.out.printf("%dm %d paths %dmsec\n", pathLength, solutions.size(), elapsed);
            }
        }

        finder = new LengthConstrainedPathFinder(e1, e2, 1000, 30, true);

        solutions = finder.solveDepthFirst();
        assertTrue(solutions.size() > 0);
        /*
         * System.out.println(solutions.size());
         * 
         * if(DEBUG_OUTPUT) { File csvOut = new File("/home/abyrd/constrain.csv"); PrintWriter pw =
         * new PrintWriter(csvOut); for (Entry<Vertex, Double> entry :
         * finder.pathProportions().entrySet()) if (entry.getKey() instanceof TurnVertex)
         * pw.printf("%f; %s \n", entry.getValue(),
         * ((TurnVertex)entry.getKey()).getGeometry().toText()); pw.close(); }
         */
    }

}
