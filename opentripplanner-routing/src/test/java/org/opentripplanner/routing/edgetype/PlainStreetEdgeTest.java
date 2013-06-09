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
    private IntersectionVertex v0, v1, v2;
    private RoutingRequest proto;

    @Before
    public void before() {
        _graph = new Graph();

        v0 = vertex("maple_0th", 0.0, 0.0);
        v1 = vertex("maple_1st", 2.0, 2.0);
        v2 = vertex("maple_2nd", 1.0, 2.0);
        v1.setTrafficLight(true);
        
        proto = new RoutingRequest();
        proto.setCarSpeed(15.0f);
        proto.setWalkSpeed(1.0);
        proto.setBikeSpeed(5.0f);
        proto.setWalkReluctance(1.0);
        proto.setStairsReluctance(1.0);
        proto.setTurnReluctance(1.0);
        proto.setModes(TraverseModeSet.allModes());
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

        RoutingRequest options = proto.clone();
        options.setMode(TraverseMode.WALK);
        options.setRoutingContext(_graph, v1, v2);
        
        State s0 = new State(options);
        State s1 = e1.traverse(s0);
        
        // Should use the speed on the edge.
        double expectedWeight = e1.getLength() / options.getWalkSpeed();
        long expectedDuration = (long) Math.ceil(expectedWeight);
        assertEquals(expectedDuration, s1.getElapsedTimeSeconds(), 0.0);
        assertEquals(expectedWeight, s1.getWeight(), 0.0);
    }
    
    @Test
    public void testTraverseAsCar() {
        PlainStreetEdge e1 = edge(v1, v2, 100.0, StreetTraversalPermission.ALL);
        e1.setCarSpeed(10.0f);

        RoutingRequest options = proto.clone();
        options.setMode(TraverseMode.CAR);
        options.setRoutingContext(_graph, v1, v2);
        
        State s0 = new State(options);
        State s1 = e1.traverse(s0);
        
        // Should use the speed on the edge.
        double expectedWeight = e1.getLength() / e1.getCarSpeed();
        long expectedDuration = (long) Math.ceil(expectedWeight);
        assertEquals(expectedDuration, s1.getElapsedTimeSeconds(), 0.0);
        assertEquals(expectedWeight, s1.getWeight(), 0.0);
    }
    
    @Test
    public void testTraverseAsCustomMotorVehicle() {
        PlainStreetEdge e1 = edge(v1, v2, 100.0, StreetTraversalPermission.ALL);
        e1.setCarSpeed(10.0f);

        RoutingRequest options = proto.clone();
        options.setMode(TraverseMode.CUSTOM_MOTOR_VEHICLE);
        options.setRoutingContext(_graph, v1, v2);
        
        State s0 = new State(options);
        State s1 = e1.traverse(s0);
        
        // Should use the speed on the edge.
        double expectedWeight = e1.getLength() / e1.getCarSpeed();
        long expectedDuration = (long) Math.ceil(expectedWeight);
        assertEquals(expectedDuration, s1.getElapsedTimeSeconds(), 0.0);
        assertEquals(expectedWeight, s1.getWeight(), 0.0);
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
    
    /**
     * Test the traversal of two edges with different traverse modes.
     * This test will fail unless the following three conditions are met:
     * 1. Turn costs are computed based on the back edge's traverse mode during reverse traversal.
     * 2. Turn costs are computed such that bike walking is taken into account correctly.
     * 3. Enabling bike mode on a routing request bases the bike walking speed on the walking speed.
     */
    @Test
    public void testTraverseModeSwitch() {
        PlainStreetEdge e0 = edge(v0, v1, 50.0, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        PlainStreetEdge e1 = edge(v1, v2, 18.4, StreetTraversalPermission.PEDESTRIAN);

        RoutingRequest forward = proto.clone();
        forward.setMode(TraverseMode.BICYCLE);
        forward.setRoutingContext(_graph, v0, v2);

        State s0 = new State(forward);
        State s1 = e0.traverse(s0);
        State s2 = e1.traverse(s1);

        RoutingRequest reverse = proto.clone();
        reverse.setMode(TraverseMode.BICYCLE);
        reverse.setArriveBy(true);
        reverse.setRoutingContext(_graph, v0, v2);

        State s3 = new State(reverse);
        State s4 = e1.traverse(s3);
        State s5 = e0.traverse(s4);

        assertEquals(42, s2.getElapsedTimeSeconds());
        assertEquals(42, s5.getElapsedTimeSeconds());
    }

    /****
     * Private Methods
     ****/

    private IntersectionVertex vertex(String label, double lat, double lon) {
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
