package org.opentripplanner.graph_builder.module.osm;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.openstreetmap.BinaryOpenStreetMapProvider;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.core.intersection_model.ConstantIntersectionTraversalCostModel;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TriangleInequalityTest {
    
    private static HashMap<Class<?>, Object> extra;
    private static Graph graph;

    private Vertex start;
    private Vertex end;

    @BeforeClass
    public static void onlyOnce() {

        extra = new HashMap<>();
        graph = new Graph();

        OpenStreetMapModule loader = new OpenStreetMapModule();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());

        File file = new File(
                URLDecoder.decode(
                        TriangleInequalityTest.class.getResource("NYC_small.osm.pbf").getFile(),
                        StandardCharsets.UTF_8
                )
        );
        DataSource source = new FileDataSource(file, FileType.OSM);
        BinaryOpenStreetMapProvider provider = new BinaryOpenStreetMapProvider(source, true);

        loader.setProvider(provider);
        loader.buildGraph(graph, extra);
    }
    
    @Before
    public void before() {
        start = graph.getVertex("osm:node:1919595913");
        end = graph.getVertex("osm:node:42448554");
    }

    private GraphPath getPath(AStar aStar, RoutingRequest proto,
            Edge startBackEdge, Vertex u, Vertex v) {
        RoutingRequest options = proto.clone();
        options.setRoutingContext(graph, startBackEdge, u, v);
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPath(v, false);
        options.cleanup();
        return path;
    }
    
    private void checkTriangleInequality() {
        checkTriangleInequality(null); 
    }
    
    private void checkTriangleInequality(TraverseModeSet traverseModes) {
        assertNotNull(start);
        assertNotNull(end);
        
        RoutingRequest prototypeOptions = new RoutingRequest();
        
        // All reluctance terms are 1.0 so that duration is monotonically increasing in weight.
        prototypeOptions.stairsReluctance = (1.0);
        prototypeOptions.setNonTransitReluctance(1.0);
        prototypeOptions.turnReluctance = (1.0);
        prototypeOptions.carSpeed = 1.0;
        prototypeOptions.walkSpeed = 1.0;
        prototypeOptions.bikeSpeed = 1.0;
        prototypeOptions.dominanceFunction = new DominanceFunction.EarliestArrival();

        graph.setIntersectionTraversalCostModel(new ConstantIntersectionTraversalCostModel(10.0));

        
        if (traverseModes != null) {
            prototypeOptions.setStreetSubRequestModes(traverseModes);
        }
        
        RoutingRequest options = prototypeOptions.clone();
        options.setRoutingContext(graph, start, end);
        
        AStar aStar = new AStar();
        
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPath(end, false);
        options.cleanup();
        assertNotNull(path);
        
        double startEndWeight = path.getWeight();
        int startEndDuration = path.getDuration();
        assertTrue(startEndWeight > 0);
        assertEquals(startEndWeight, (double) startEndDuration, 1.0 * path.edges.size());
        
        // Try every vertex in the graph as an intermediate.
        boolean violated = false;
        for (Vertex intermediate : graph.getVertices()) {
            if (intermediate == start || intermediate == end) {
                continue;
            }
            
            GraphPath startIntermediatePath = getPath(aStar, prototypeOptions, null, start, intermediate);
            if (startIntermediatePath == null) {
                continue;
            }
            
            Edge back = startIntermediatePath.states.getLast().getBackEdge();
            GraphPath intermediateEndPath = getPath(aStar, prototypeOptions, back, intermediate, end);
            if (intermediateEndPath == null) {
                continue;
            }
            
            double startIntermediateWeight = startIntermediatePath.getWeight();
            int startIntermediateDuration = startIntermediatePath.getDuration();
            double intermediateEndWeight = intermediateEndPath.getWeight();
            int intermediateEndDuration = intermediateEndPath.getDuration();
            
            // TODO(flamholz): fix traversal so that there's no rounding at the second resolution.
            assertEquals(startIntermediateWeight, (double) startIntermediateDuration,
                    1.0 * startIntermediatePath.edges.size());            
            assertEquals(intermediateEndWeight, (double) intermediateEndDuration,
                    1.0 * intermediateEndPath.edges.size());
            
            double diff = startIntermediateWeight + intermediateEndWeight - startEndWeight;
            if (diff < -0.01) {
                System.out.println("Triangle inequality violated - diff = " + diff);
                violated = true;
            }
            //assertTrue(startIntermediateDuration + intermediateEndDuration >=
            //        startEndDuration);
        }
        
        assertFalse(violated);
    }

    @Test
    public void testTriangleInequalityDefaultModes() {
        checkTriangleInequality();
    }
    
    @Test
    public void testTriangleInequalityWalkingOnly() {
        TraverseModeSet modes = new TraverseModeSet(TraverseMode.WALK);
        checkTriangleInequality(modes);
    }

    @Test
    public void testTriangleInequalityDrivingOnly() {
        TraverseModeSet modes = new TraverseModeSet(TraverseMode.CAR);
        checkTriangleInequality(modes);
    }
    
    @Test
    public void testTriangleInequalityWalkTransit() {
        TraverseModeSet modes = new TraverseModeSet(TraverseMode.WALK,
                TraverseMode.TRANSIT);
        checkTriangleInequality(modes);
    }
    
    @Test
    public void testTriangleInequalityWalkBike() {
        TraverseModeSet modes = new TraverseModeSet(TraverseMode.WALK,
                TraverseMode.BICYCLE);
        checkTriangleInequality(modes);
    }
    
    @Test
    public void testTriangleInequalityDefaultModesBasicSPT() {
        checkTriangleInequality(null);
    }
    
    @Test
    public void testTriangleInequalityWalkingOnlyBasicSPT() {
        TraverseModeSet modes = new TraverseModeSet(TraverseMode.WALK);
        checkTriangleInequality(modes);
    }

    @Test
    public void testTriangleInequalityDrivingOnlyBasicSPT() {
        TraverseModeSet modes = new TraverseModeSet(TraverseMode.CAR);
        checkTriangleInequality(modes);
    }
    
    @Test
    public void testTriangleInequalityWalkTransitBasicSPT() {
        TraverseModeSet modes = new TraverseModeSet(TraverseMode.WALK,
                TraverseMode.TRANSIT);
        checkTriangleInequality(modes);
    }
    
    @Test
    public void testTriangleInequalityWalkBikeBasicSPT() {
        TraverseModeSet modes = new TraverseModeSet(TraverseMode.WALK,
                TraverseMode.BICYCLE);
        checkTriangleInequality(modes);
    }
    
    @Test
    public void testTriangleInequalityDefaultModesMultiSPT() {
        checkTriangleInequality(null);
    }
    
    @Test
    public void testTriangleInequalityWalkingOnlyMultiSPT() {
        TraverseModeSet modes = new TraverseModeSet(TraverseMode.WALK);
        checkTriangleInequality(modes);
    }

    @Test
    public void testTriangleInequalityDrivingOnlyMultiSPT() {
        TraverseModeSet modes = new TraverseModeSet(TraverseMode.CAR);
        checkTriangleInequality(modes);
    }
    
    @Test
    public void testTriangleInequalityWalkTransitMultiSPT() {
        TraverseModeSet modes = new TraverseModeSet(TraverseMode.WALK,
                TraverseMode.TRANSIT);
        checkTriangleInequality(modes);
    }
    
    @Test
    public void testTriangleInequalityWalkBikeMultiSPT() {
        TraverseModeSet modes = new TraverseModeSet(TraverseMode.WALK,
                TraverseMode.BICYCLE);
        checkTriangleInequality(modes);
    }
}
