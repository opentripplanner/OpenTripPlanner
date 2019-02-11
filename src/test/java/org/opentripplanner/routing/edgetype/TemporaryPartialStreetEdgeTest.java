package org.opentripplanner.routing.edgetype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.util.NonLocalizedString;

public class TemporaryPartialStreetEdgeTest {
    
    private Graph graph;
    private IntersectionVertex v1, v2, v3, v4;
    private StreetEdge e1, e1Reverse, e2, e3;
    
    @Before
    public void setUp() throws Exception {
        graph = new Graph();

        // Graph for a fictional grid city with turn restrictions
        v1 = vertex("maple_1st", 2.0, 2.0);
        v2 = vertex("maple_2nd", 1.0, 2.0);
        v3 = vertex("maple_3rd", 0.0, 2.0);
        v4 = vertex("maple_4th", -1.0, 2.0);
        
        e1 = edge(v1, v2, 1.0, StreetTraversalPermission.ALL);
        e1Reverse = edge(v2, v1, 1.0, StreetTraversalPermission.ALL);
        e2 = edge(v2, v3, 1.0, StreetTraversalPermission.ALL);
        e3 = edge(v3, v4, 1.0, StreetTraversalPermission.ALL);
    }

    @Test
    public void testConstruction() {
        TemporaryPartialStreetEdge pEdge = newTemporaryPartialStreetEdge(e1, v1, v2, e1.getGeometry(),
                "partial e1", e1.getDistance());

        assertTrue(pEdge.isEquivalentTo(e1));
        assertTrue(pEdge.isPartial());
        assertFalse(pEdge.isBack());
        assertFalse(pEdge.isReverseOf(e1));
        assertTrue(pEdge.isReverseOf(e1Reverse));
        assertEquals(e1.getId(), pEdge.getId());
        assertEquals(e1.getPermission(), pEdge.getPermission());
        assertEquals(e1.getCarSpeed(), pEdge.getCarSpeed(), 0.0);
    }
    
    @Test
    public void testTraversal() {
        RoutingRequest options = new RoutingRequest();
        options.setMode(TraverseMode.CAR);
        options.setRoutingContext(graph, v1, v2);

        // Partial edge with same endpoints as the parent.
        TemporaryPartialStreetEdge pEdge1 = newTemporaryPartialStreetEdge(e1, v1, v2, e1.getGeometry(),
                "partial e1", e1.getDistance());
        TemporaryPartialStreetEdge pEdge2 = newTemporaryPartialStreetEdge(e2, v2, v3, e2.getGeometry(),
                "partial e2", e2.getDistance());

        // Traverse both the partial and parent edges.
        State s0 = new State(options);
        State s1 = e1.traverse(s0);
        
        State partialS0 = new State(options);
        State partialS1 = pEdge1.traverse(partialS0);
        
        // Traversal of original and partial edges should yield the same results.
        assertEquals(s1.getTimeSeconds(), partialS1.getTimeSeconds());
        assertEquals(s1.getElapsedTimeSeconds(), partialS1.getElapsedTimeSeconds());
        assertEquals(s1.getWeight(), partialS1.getWeight(), 0.0);
        
        // Now traverse the second partial/parent edge pair.
        State s2 = e2.traverse(s1);
        State partialS2 = pEdge2.traverse(partialS1);
        
        // Same checks as above.
        assertEquals(s2.getTimeSeconds(), partialS2.getTimeSeconds());
        assertEquals(s2.getElapsedTimeSeconds(), partialS2.getElapsedTimeSeconds());
        assertEquals(s2.getWeight(), partialS2.getWeight(), 0.0);
    }
    
    @Test
    public void testTraversalOfSubdividedEdge() {
        Coordinate nearestPoint = new Coordinate(0.5, 2.0);
        List<StreetEdge> edges = new ArrayList<StreetEdge>();
        edges.add(e2);
        TemporaryStreetLocation end = StreetVertexIndexServiceImpl.createTemporaryStreetLocation(
                graph, "middle of e2", new NonLocalizedString("foo"), edges, nearestPoint, true);
        TemporaryStreetLocation start = StreetVertexIndexServiceImpl.createTemporaryStreetLocation(
                graph, "middle of e2", new NonLocalizedString("foo"), edges, nearestPoint, false);

        RoutingRequest options = new RoutingRequest();
        options.setMode(TraverseMode.CAR);
        options.setRoutingContext(graph, v1, v2);
        options.rctx.temporaryVertices.addAll(Arrays.asList(end, start));

        // All intersections take 10 minutes - we'll notice if one isn't counted.
        double turnDurationSecs = 10.0 * 60.0;  
        options.traversalCostModel = (new DummyCostModel(turnDurationSecs));
        options.turnReluctance = (1.0);
        
        State s0 = new State(options);
        State s1 = e1.traverse(s0);
        State s2 = e2.traverse(s1);
        State s3 = e3.traverse(s2);
        
        Edge partialE2First = end.getIncoming().iterator().next();
        Edge partialE2Second = start.getOutgoing().iterator().next();

        State partialS0 = new State(options);
        State partialS1 = e1.traverse(partialS0);
        State partialS2A = partialE2First.traverse(partialS1);
        State partialS2B = partialE2Second.traverse(partialS2A);
        State partialS3 = e3.traverse(partialS2B);
        
        // Should start at the same time.
        assertEquals(s0.getTimeSeconds(), partialS0.getTimeSeconds());
        
        // Time and cost should be the same up to a rounding difference.
        assertTrue(Math.abs(s3.getTimeSeconds() - partialS3.getTimeSeconds()) <= 1);
        assertTrue(Math.abs(s3.getElapsedTimeSeconds() - partialS3.getElapsedTimeSeconds()) <= 1);
        assertTrue(Math.abs(s3.getWeight() - partialS3.getWeight()) <= 1);
        
        // All intersections take 0 seconds now.
        options.traversalCostModel = (new DummyCostModel(0.0));

        State s0NoCost = new State(options);
        State s1NoCost = e1.traverse(s0NoCost);
        State s2NoCost = e2.traverse(s1NoCost);
        State s3NoCost = e3.traverse(s2NoCost);
        
        State partialS0NoCost = new State(options);
        State partialS1NoCost = e1.traverse(partialS0NoCost);
        State partialS2ANoCost = partialE2First.traverse(partialS1NoCost);
        State partialS2BNoCost = partialE2Second.traverse(partialS2ANoCost);
        State partialS3NoCost = e3.traverse(partialS2BNoCost);
        
        // Time and cost should be the same up to a rounding difference.
        assertTrue(Math.abs(s3NoCost.getTimeSeconds() - partialS3NoCost.getTimeSeconds()) <= 1);
        assertTrue(Math.abs(s3NoCost.getElapsedTimeSeconds() - partialS3NoCost.getElapsedTimeSeconds()) <= 1);
        assertTrue(Math.abs(s3NoCost.getWeight() - partialS3NoCost.getWeight()) <= 1);
        
        // Difference in duration and weight between now and before should be
        // entirely due to the crossing of 2 intersections at v2 and v3.
        double expectedDifference = 2 * 10 * 60.0;
        double durationDiff = s3.getTimeSeconds() - s3NoCost.getTimeSeconds();
        double partialDurationDiff = partialS3.getTimeSeconds() - partialS3NoCost.getTimeSeconds();
        assertTrue(Math.abs(durationDiff - expectedDifference) <= 1);
        assertTrue(Math.abs(partialDurationDiff - expectedDifference) <= 1);
        
        // Turn reluctance is 1.0, so weight == duration.
        double weightDiff = s3.getWeight() - s3NoCost.getWeight();
        double partialWeightDiff = partialS3.getWeight() - partialS3NoCost.getWeight();
        assertTrue(Math.abs(weightDiff - expectedDifference) <= 1);
        assertTrue(Math.abs(partialWeightDiff - expectedDifference) <= 1);
    }
    
    @Test
    public void testReverseEdge() {
        TemporaryPartialStreetEdge pEdge1 = newTemporaryPartialStreetEdge(e1, v1, v2, e1.getGeometry(),
                "partial e1", e1.getDistance());
        TemporaryPartialStreetEdge pEdge2 = newTemporaryPartialStreetEdge(e1Reverse, v2, v1, e1Reverse.getGeometry(),
                "partial e2", e1Reverse.getDistance());
        
        assertFalse(e1.isReverseOf(pEdge1));
        assertFalse(pEdge1.isReverseOf(e1));
        
        assertFalse(e1Reverse.isReverseOf(pEdge2));
        assertFalse(pEdge2.isReverseOf(e1Reverse));
        
        assertTrue(e1.isReverseOf(pEdge2));
        assertTrue(e1Reverse.isReverseOf(pEdge1));
        assertTrue(e1Reverse.isReverseOf(e1));
        assertTrue(e1.isReverseOf(e1Reverse));
        assertTrue(pEdge1.isReverseOf(pEdge2));
        assertTrue(pEdge2.isReverseOf(pEdge1));
    }
    
    /* Private Methods */

    static TemporaryPartialStreetEdge newTemporaryPartialStreetEdge(StreetEdge parentEdge, StreetVertex v1, StreetVertex v2, LineString geometry, String name, double length) {
        return new TemporaryPartialStreetEdge(parentEdge, v1, v2, geometry, new NonLocalizedString(name), length);
    }

    private IntersectionVertex vertex(String label, double lat, double lon) {
        IntersectionVertex v = new IntersectionVertex(graph, label, lat, lon);
        return v;
    }

    /**
     * Create an edge. If twoWay, create two edges (back and forth).
     * 
     * @param vA
     * @param vB
     * @param length
     */
    private StreetEdge edge(StreetVertex vA, StreetVertex vB, double length,
            StreetTraversalPermission perm) {
        String labelA = vA.getLabel();
        String labelB = vB.getLabel();
        String name = String.format("%s_%s", labelA, labelB);
        Coordinate[] coords = new Coordinate[2];
        coords[0] = vA.getCoordinate();
        coords[1] = vB.getCoordinate();
        LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

        return new StreetEdge(vA, vB, geom, name, length, perm, false);
    }
    
    /**
     * Dummy cost model. Returns what you put in.
     */
    private static class DummyCostModel implements IntersectionTraversalCostModel {

        private double turnCostSecs;
        
        public DummyCostModel(double turnCostSecs) {
            this.turnCostSecs = turnCostSecs;
        }
        
        @Override
        public double computeTraversalCost(IntersectionVertex v, StreetEdge from,
                StreetEdge to, TraverseMode mode, RoutingRequest options, float fromSpeed,
                float toSpeed) {
            return this.turnCostSecs;
        }
    }

}
