package org.opentripplanner.routing.vertextype;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public class IntersectionVertexTest {

    private Graph _graph;

    private PlainStreetEdge fromEdge;
    private PlainStreetEdge straightAheadEdge;
    private PlainStreetEdge turnEdge;
    
    @Before
    public void before() {
        _graph = new Graph();

        // Graph for a fictional grid city with turn restrictions
        StreetVertex maple1 = vertex("maple_1st", 2.0, 2.0);
        StreetVertex maple2 = vertex("maple_2nd", 1.0, 2.0);
        StreetVertex maple3 = vertex("maple_3rd", 0.0, 2.0);

        StreetVertex main1 = vertex("main_1st", 2.0, 1.0);
        StreetVertex main2 = vertex("main_2nd", 1.0, 1.0);
        StreetVertex main3 = vertex("main_3rd", 0.0, 1.0);

        StreetVertex broad1 = vertex("broad_1st", 2.0, 0.0);
        StreetVertex broad2 = vertex("broad_2nd", 1.0, 0.0);
        StreetVertex broad3 = vertex("broad_3rd", 0.0, 0.0);

        // Each block along the main streets has unit length and is one-way
        PlainStreetEdge maple1_2 = edge(maple1, maple2, 100.0, false);
        PlainStreetEdge maple2_3 = edge(maple2, maple3, 100.0, false);

        PlainStreetEdge main1_2 = edge(main1, main2, 100.0, false);
        PlainStreetEdge main2_3 = edge(main2, main3, 100.0, false);

        PlainStreetEdge broad1_2 = edge(broad1, broad2, 100.0, false);
        PlainStreetEdge broad2_3 = edge(broad2, broad3, 100.0, false);

        // Each cross-street connects
        PlainStreetEdge maple_main1 = edge(maple1, main1, 50.0, false);
        PlainStreetEdge main_broad1 = edge(main1, broad1, 100.0, false);

        PlainStreetEdge maple_main2 = edge(maple2, main2, 50.0, false);
        PlainStreetEdge main_broad2 = edge(main2, broad2, 50.0, false);

        PlainStreetEdge maple_main3 = edge(maple3, main3, 100.0, false);
        PlainStreetEdge main_broad3 = edge(main3, broad3, 100.0, false);

        this.fromEdge = maple1_2;
        this.straightAheadEdge = maple2_3;
        this.turnEdge = maple_main2;
    }

    @Test
    public void testFreeFlowing() {
        IntersectionVertex iv = new IntersectionVertex(_graph, "vertex", 1.0, 2.0);
        assertFalse(iv.isFreeFlowing());
        
        iv.setFreeFlowing(true);
        assertTrue(iv.isFreeFlowing());

        TraverseMode mode = TraverseMode.BICYCLE;
        RoutingRequest options = new RoutingRequest();
        float fromSpeed = 4.5f;
        float toSpeed = 4.5f;
        assertEquals(0.0,
                iv.computeTraversalCost(fromEdge, straightAheadEdge, mode, options, fromSpeed, toSpeed), 0.0);
    }

    @Test
    public void testNotFreeFlowingBike() {
        IntersectionVertex iv = new IntersectionVertex(_graph, "vertex", 1.0, 2.0);
        assertFalse(iv.isFreeFlowing());
        
        TraverseMode mode = TraverseMode.BICYCLE;
        RoutingRequest options = new RoutingRequest();
        float fromSpeed = 4.5f;
        float toSpeed = 4.5f;
        
        // Traversal cost is 0 because we're continuing straight ahead on a bike.
        double straightAheadCost = iv.computeTraversalCost(fromEdge, straightAheadEdge, mode, options, fromSpeed, toSpeed);
        assertEquals(0.0, straightAheadCost, 0.0);
        
        // If we are turning, then there is some cost due to the turn.
        double turnCost = iv.computeTraversalCost(fromEdge, turnEdge, mode, options, fromSpeed, toSpeed);
        assertTrue(turnCost > 0.0);
    }

    @Test
    public void testNotFreeFlowingCar() {
        IntersectionVertex iv = new IntersectionVertex(_graph, "vertex", 1.0, 2.0);
        assertFalse(iv.isFreeFlowing());
        
        TraverseMode mode = TraverseMode.CAR;
        RoutingRequest options = new RoutingRequest();
        float fromSpeed = 4.5f;
        float toSpeed = 4.5f;
        
        // Traversal cost is greater than 0 because we're driving.
        double traversalCost = iv.computeTraversalCost(fromEdge, straightAheadEdge, mode, options, fromSpeed, toSpeed);
        assertTrue(traversalCost > 0.0);
    
        // If there's a turn, the cost should be higher.
        double turnCost = iv.computeTraversalCost(fromEdge, turnEdge, mode, options, fromSpeed, toSpeed);
        assertTrue(turnCost > 0.0);
        assertTrue(turnCost > traversalCost);
    }
    
    /****
     * Private Methods
     ****/

    private StreetVertex vertex(String label, double lat, double lon) {
        IntersectionVertex v = new IntersectionVertex(_graph, label, lat, lon);
        return v;
    }

    /**
     * Create an edge. If twoWay, create two edges (back and forth).
     * 
     * @param vA
     * @param vB
     * @param length
     * @param back true if this is a reverse edge
     */
    private PlainStreetEdge edge(StreetVertex vA, StreetVertex vB, double length, boolean back) {
        String labelA = vA.getLabel();
        String labelB = vB.getLabel();
        String name = String.format("%s_%s", labelA, labelB);
        Coordinate[] coords = new Coordinate[2];
        coords[0] = vA.getCoordinate();
        coords[1] = vB.getCoordinate();
        LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

        StreetTraversalPermission perm = StreetTraversalPermission.ALL;
        return new PlainStreetEdge(vA, vB, geom, name, length, perm, back);
    }
}
