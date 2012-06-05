package org.opentripplanner.graph_builder.impl.osm;

import java.io.File;
import java.util.HashMap;
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

    @Test
    public void testGraphBuilder() throws Exception {
        
        Graph gg = new Graph();

        OpenStreetMapGraphBuilderImpl loader = new OpenStreetMapGraphBuilderImpl();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        FileBasedOpenStreetMapProviderImpl provider = new FileBasedOpenStreetMapProviderImpl();
        File file = new File(getClass().getResource("map.osm.gz").getFile());
        provider.setPath(file);
        loader.setProvider(provider);
        loader.buildGraph(gg, extra);

        Vertex v2 = gg.getVertex("way 25660216 from 1"); // Kamiennogorska
        Vertex v2back = gg.getVertex("way 25660216 from 1 back"); // Kamiennogorska
                                                                  // back
        assertNotNull(v2);
        assertNotNull(v2back);
        
        Edge startEdge = v2.getOutgoing().iterator().next();
        Edge endEdge = v2.getIncoming().iterator().next();
        assertNotNull(startEdge);
        assertNotNull(endEdge);
        System.out.println(startEdge);
        System.out.println(endEdge);

        LengthConstrainedPathFinder finder;
        Set<State> solutions;
        
        for (boolean prune : new boolean[] {false, true}) {
            System.out.printf("%s\n", prune);
            long elapsed = 0;
            for (int pathLength = 100; elapsed < 2000 && pathLength < 4000; pathLength += 50) {
                long t0 = System.currentTimeMillis();
                finder = new LengthConstrainedPathFinder(startEdge, startEdge, pathLength, 0, prune);
                solutions = finder.solveBreadthFirst();
                long t1 = System.currentTimeMillis();
                elapsed = t1 - t0;
                System.out.printf("%dm %d paths %dmsec\n", pathLength, solutions.size(), elapsed);
            }   
        }

        finder = new LengthConstrainedPathFinder(startEdge, startEdge, 1800, 0, true);
        solutions = finder.solveDepthFirst();
        System.out.println(solutions.size());
        for (State s : solutions)
            System.out.println(s.toStringVerbose());

        solutions = finder.solveBreadthFirst();
        System.out.println(solutions.size());
        for (State s : solutions)
            System.out.println(s.toStringVerbose());
        
    }
    
}
