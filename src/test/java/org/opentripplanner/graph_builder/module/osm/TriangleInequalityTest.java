/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.osm;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.net.URLDecoder;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.openstreetmap.impl.FileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.ConstantIntersectionTraversalCostModel;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class TriangleInequalityTest {
    
    private static HashMap<Class<?>, Object> extra;
    private static Graph _graph;

    private Vertex start;
    private Vertex end;

    @BeforeClass
    public static void onlyOnce() throws Exception {

        extra = new HashMap<Class<?>, Object>();
        _graph = new Graph();

        OpenStreetMapModule loader = new OpenStreetMapModule();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        FileBasedOpenStreetMapProviderImpl provider = new FileBasedOpenStreetMapProviderImpl();

        File file = new File(URLDecoder.decode(TriangleInequalityTest.class.getResource("NYC_small.osm.gz").getFile(), "UTF-8"));

        provider.setPath(file);
        loader.setProvider(provider);
        loader.buildGraph(_graph, extra);

        // Need to set up the index because buildGraph doesn't do it.
        _graph.rebuildVertexAndEdgeIndices();
    }
    
    @Before
    public void before() {
        start = _graph.getVertex("osm:node:1919595913");        
        end = _graph.getVertex("osm:node:42448554");    
    }

    private GraphPath getPath(AStar aStar, RoutingRequest proto,
            Edge startBackEdge, Vertex u, Vertex v) {
        RoutingRequest options = proto.clone();
        options.setRoutingContext(_graph, startBackEdge, u, v);
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
        prototypeOptions.setWalkReluctance(1.0);
        prototypeOptions.turnReluctance = (1.0);
        prototypeOptions.carSpeed = 1.0;
        prototypeOptions.walkSpeed = 1.0;
        prototypeOptions.bikeSpeed = 1.0;
        prototypeOptions.traversalCostModel = (new ConstantIntersectionTraversalCostModel(10.0));
        prototypeOptions.dominanceFunction = new DominanceFunction.EarliestArrival();

        
        if (traverseModes != null) {
            prototypeOptions.setModes(traverseModes);
        }
        
        RoutingRequest options = prototypeOptions.clone();
        options.setRoutingContext(_graph, start, end);
        
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
        for (Vertex intermediate : _graph.getVertices()) {
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
