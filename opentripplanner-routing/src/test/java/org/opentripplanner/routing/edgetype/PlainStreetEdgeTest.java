package org.opentripplanner.routing.edgetype;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public class PlainStreetEdgeTest {

    private Graph _graph;
    private StreetVertex v1, v2;

    @Before
    public void before() {
        _graph = new Graph();

        v1 = vertex("maple_1st", 2.0, 2.0);
        v2 = vertex("maple_2nd", 1.0, 2.0);
    }
    
    @Test
    public void testInAndOutAngles() {
        PlainStreetEdge e1 = edge(v1, v2, 1.0, StreetTraversalPermission.ALL);
        
        // Edge has same first and last angle.
        assertEquals(91, e1.getInAngle());
        assertEquals(91, e1.getOutAngle());
        
        // 2 new ones
        StreetVertex u = vertex("test1", 2.0, 1.0);
        StreetVertex v = vertex("test2", 2.0, 2.0);
        
        // Second edge
        PlainStreetEdge e2 = edge(u, v, 1.0, StreetTraversalPermission.ALL);

        assertEquals(180, e2.getInAngle());
        assertEquals(180, e2.getOutAngle());
        
        // Difference should be about 90.
        int diff = (e1.getOutAngle() - e2.getInAngle());
        assertEquals(-89, diff);
    }

    @Test
    public void testTraverseAsPedestrian() {
        PlainStreetEdge e1 = edge(v1, v2, 100.0, StreetTraversalPermission.ALL);
        e1.setCarSpeed(10.0f);

        RoutingRequest options = new RoutingRequest();
        options.setMode(TraverseMode.WALK);
        options.setCarSpeed(15.0f);
        options.setWalkSpeed(1.0);
        options.setRoutingContext(_graph, v1, v2);
        
        State s0 = new State(options);
        State s1 = e1.traverse(s0);
        
        // Should use the speed on the edge.
        long expectedDuration = (long) Math.ceil(e1.getLength() / options.getWalkSpeed());
        assertEquals(expectedDuration, s1.getElapsedTimeSeconds(), 0.0);
    }
    
    @Test
    public void testTraverseAsCar() {
        PlainStreetEdge e1 = edge(v1, v2, 100.0, StreetTraversalPermission.ALL);
        e1.setCarSpeed(10.0f);

        RoutingRequest options = new RoutingRequest();
        options.setMode(TraverseMode.CAR);
        options.setCarSpeed(15.0f);
        options.setRoutingContext(_graph, v1, v2);
        
        State s0 = new State(options);
        State s1 = e1.traverse(s0);
        
        // Should use the speed on the edge.
        long expectedDuration = (long) Math.ceil(e1.getLength() / e1.getCarSpeed());
        assertEquals(expectedDuration, s1.getElapsedTimeSeconds(), 0.0);
    }
    
    @Test
    public void testTraverseAsCustomMotorVehicle() {
        PlainStreetEdge e1 = edge(v1, v2, 100.0, StreetTraversalPermission.ALL);
        e1.setCarSpeed(10.0f);

        RoutingRequest options = new RoutingRequest();
        options.setMode(TraverseMode.CUSTOM_MOTOR_VEHICLE);
        options.setCarSpeed(15.0f);
        options.setRoutingContext(_graph, v1, v2);
        
        State s0 = new State(options);
        State s1 = e1.traverse(s0);
        
        // Should use the speed on the edge.
        long expectedDuration = (long) Math.ceil(e1.getLength() / e1.getCarSpeed());
        assertEquals(expectedDuration, s1.getElapsedTimeSeconds(), 0.0);
    }
    
    @Test
    public void testModeSetCanTraverse() {
        PlainStreetEdge e = edge(v1, v2, 1.0, StreetTraversalPermission.ALL);
        
        TraverseModeSet modes = TraverseModeSet.allModes();
        assertTrue(e.canTraverse(modes));
        
        modes = new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK);
        assertTrue(e.canTraverse(modes));
        
        e = edge(v1, v2, 1.0, StreetTraversalPermission.ALL_DRIVING);
        assertFalse(e.canTraverse(modes));
        
        modes = new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK);
        assertTrue(e.canTraverse(modes));
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
    private PlainStreetEdge edge(StreetVertex vA, StreetVertex vB, double length,
            StreetTraversalPermission perm) {
        String labelA = vA.getLabel();
        String labelB = vB.getLabel();
        String name = String.format("%s_%s", labelA, labelB);
        Coordinate[] coords = new Coordinate[2];
        coords[0] = vA.getCoordinate();
        coords[1] = vB.getCoordinate();
        LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

        return new PlainStreetEdge(vA, vB, geom, name, length, perm, false);
    }

}
