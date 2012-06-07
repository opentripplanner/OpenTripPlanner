package org.opentripplanner.graph_builder.impl.osm;

import java.io.File;
import java.io.FileOutputStream;
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
        gg.save(new File("/home/abyrd/constrain.graph"));
        
        Vertex v1 = gg.getVertex("way 27331296 from 3"); 
        Vertex v2 = gg.getVertex("way 27339447 from 2");
        assertNotNull(v1);
        assertNotNull(v2);
        
        Edge startEdge = v1.getOutgoing().iterator().next();
        Edge endEdge = v2.getOutgoing().iterator().next();
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
                finder = new LengthConstrainedPathFinder(startEdge, endEdge, pathLength, 0, prune);
                solutions = finder.solveBreadthFirst();
                long t1 = System.currentTimeMillis();
                elapsed = t1 - t0;
                System.out.printf("%dm %d paths %dmsec\n", pathLength, solutions.size(), elapsed);
            }   
        }

        finder = new LengthConstrainedPathFinder(startEdge, endEdge, 2500, 0, true);
        
        solutions = finder.solveDepthFirst();
        System.out.println(solutions.size());
//        for (State s : solutions)
//            System.out.println(s.toStringVerbose());

        solutions = finder.solveBreadthFirst();
        System.out.println(solutions.size());
//        for (State s : solutions)
//            System.out.println(s.toStringVerbose());
        
        File csvOut = new File("/home/abyrd/constrain.csv");
        PrintWriter pw = new PrintWriter(csvOut);
        for (Entry<Edge, Double> entry : finder.pathProportions().entrySet()) {
            pw.printf("%f; %s \n", entry.getValue(), entry.getKey().getGeometry().toText());
        }
        pw.close();
        
    }
    
}
