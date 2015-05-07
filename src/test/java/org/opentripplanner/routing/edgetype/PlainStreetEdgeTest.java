/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.TurnRestriction;
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

        v0 = vertex("maple_0th", 0.0, 0.0); // label, X, Y
        v1 = vertex("maple_1st", 2.0, 2.0);
        v2 = vertex("maple_2nd", 1.0, 2.0);
        
        proto = new RoutingRequest();
        proto.setDummyRoutingContext(_graph);
        proto.carSpeed = 15.0f;
        proto.walkSpeed = 1.0;
        proto.bikeSpeed = 5.0f;
        proto.setWalkReluctance(1.0);
        proto.stairsReluctance = (1.0);
        proto.turnReluctance = (1.0);
        proto.setModes(TraverseModeSet.allModes());
    }
    
    @Test
    public void testInAndOutAngles() {
        // An edge heading straight West
        StreetEdge e1 = edge(v1, v2, 1.0, StreetTraversalPermission.ALL);
        
        // Edge has same first and last angle.
        assertEquals(90, e1.getInAngle());
        assertEquals(90, e1.getOutAngle());
        
        // 2 new ones
        StreetVertex u = vertex("test1", 2.0, 1.0);
        StreetVertex v = vertex("test2", 2.0, 2.0);
        
        // Second edge, heading straight North
        StreetEdge e2 = edge(u, v, 1.0, StreetTraversalPermission.ALL);

        // 180 degrees could be expressed as 180 or -180. Our implementation happens to use -180.
        assertEquals(180, Math.abs(e2.getInAngle()));
        assertEquals(180, Math.abs(e2.getOutAngle()));
    }

    @Test
    public void testTraverseAsPedestrian() {
        StreetEdge e1 = edge(v1, v2, 100.0, StreetTraversalPermission.ALL);
        e1.setCarSpeed(10.0f);

        RoutingRequest options = proto.clone();
        options.setMode(TraverseMode.WALK);
        options.setRoutingContext(_graph, v1, v2);
        
        State s0 = new State(options);
        State s1 = e1.traverse(s0);
        
        // Should use the speed on the edge.
        double expectedWeight = e1.getDistance() / options.walkSpeed;
        long expectedDuration = (long) Math.ceil(expectedWeight);
        assertEquals(expectedDuration, s1.getElapsedTimeSeconds(), 0.0);
        assertEquals(expectedWeight, s1.getWeight(), 0.0);
    }
    
    @Test
    public void testTraverseAsCar() {
        StreetEdge e1 = edge(v1, v2, 100.0, StreetTraversalPermission.ALL);
        e1.setCarSpeed(10.0f);

        RoutingRequest options = proto.clone();
        options.setMode(TraverseMode.CAR);
        options.setRoutingContext(_graph, v1, v2);
        
        State s0 = new State(options);
        State s1 = e1.traverse(s0);
        
        // Should use the speed on the edge.
        double expectedWeight = e1.getDistance() / e1.getCarSpeed();
        long expectedDuration = (long) Math.ceil(expectedWeight);
        assertEquals(expectedDuration, s1.getElapsedTimeSeconds(), 0.0);
        assertEquals(expectedWeight, s1.getWeight(), 0.0);
    }
    
    @Test
    public void testModeSetCanTraverse() {
        StreetEdge e = edge(v1, v2, 1.0, StreetTraversalPermission.ALL);
        
        TraverseModeSet modes = TraverseModeSet.allModes();
        assertTrue(e.canTraverse(modes));
        
        modes = new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK);
        assertTrue(e.canTraverse(modes));
        
        e = edge(v1, v2, 1.0, StreetTraversalPermission.CAR);
        assertFalse(e.canTraverse(modes));
        
        modes = new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK);
        assertTrue(e.canTraverse(modes));
    }
    
    /**
     * Test the traversal of two edges with different traverse modes, with a focus on cycling.
     * This test will fail unless the following three conditions are met:
     * 1. Turn costs are computed based on the back edge's traverse mode during reverse traversal.
     * 2. Turn costs are computed such that bike walking is taken into account correctly.
     * 3. User-specified bike speeds are applied correctly during turn cost computation.
     */
    @Test
    public void testTraverseModeSwitchBike() {
        StreetEdge e0 = edge(v0, v1, 50.0, StreetTraversalPermission.PEDESTRIAN);
        StreetEdge e1 = edge(v1, v2, 18.4, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

        v1.trafficLight = (true);

        RoutingRequest forward = proto.clone();
        forward.setMode(TraverseMode.BICYCLE);
        forward.bikeSpeed = 3.0f;
        forward.setRoutingContext(_graph, v0, v2);

        State s0 = new State(forward);
        State s1 = e0.traverse(s0);
        State s2 = e1.traverse(s1);

        RoutingRequest reverse = proto.clone();
        reverse.setMode(TraverseMode.BICYCLE);
        reverse.setArriveBy(true);
        reverse.bikeSpeed = 3.0f;
        reverse.setRoutingContext(_graph, v0, v2);

        State s3 = new State(reverse);
        State s4 = e1.traverse(s3);
        State s5 = e0.traverse(s4);

        assertEquals(73, s2.getElapsedTimeSeconds());
        assertEquals(73, s5.getElapsedTimeSeconds());
    }

    /**
     * Test the traversal of two edges with different traverse modes, with a focus on walking.
     * This test will fail unless the following three conditions are met:
     * 1. Turn costs are computed based on the back edge's traverse mode during reverse traversal.
     * 2. Turn costs are computed such that bike walking is taken into account correctly.
     * 3. Enabling bike mode on a routing request bases the bike walking speed on the walking speed.
     */
    @Test
    public void testTraverseModeSwitchWalk() {
        StreetEdge e0 = edge(v0, v1, 50.0, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        StreetEdge e1 = edge(v1, v2, 18.4, StreetTraversalPermission.PEDESTRIAN);

        v1.trafficLight = (true);

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

    /**
     * Test the bike switching penalty feature, both its cost penalty and its separate time penalty.
     */
    @Test
    public void testBikeSwitch() {
        StreetEdge e0 = edge(v0, v1, 0.0, StreetTraversalPermission.PEDESTRIAN);
        StreetEdge e1 = edge(v1, v2, 0.0, StreetTraversalPermission.BICYCLE);
        StreetEdge e2 = edge(v2, v0, 0.0, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

        RoutingRequest noPenalty = proto.clone();
        noPenalty.setMode(TraverseMode.BICYCLE);
        noPenalty.setRoutingContext(_graph, v0, v0);

        State s0 = new State(noPenalty);
        State s1 = e0.traverse(s0);
        State s2 = e1.traverse(s1);
        State s3 = e2.traverse(s2);

        RoutingRequest withPenalty = proto.clone();
        withPenalty.bikeSwitchTime = (42);
        withPenalty.bikeSwitchCost = (23);
        withPenalty.setMode(TraverseMode.BICYCLE);
        withPenalty.setRoutingContext(_graph, v0, v0);

        State s4 = new State(withPenalty);
        State s5 = e0.traverse(s4);
        State s6 = e1.traverse(s5);
        State s7 = e2.traverse(s6);

        assertEquals(0, s0.getElapsedTimeSeconds());
        assertEquals(0, s1.getElapsedTimeSeconds());
        assertEquals(0, s2.getElapsedTimeSeconds());
        assertEquals(0, s3.getElapsedTimeSeconds());

        assertEquals(0.0, s0.getWeight(), 0.0);
        assertEquals(0.0, s1.getWeight(), 0.0);
        assertEquals(0.0, s2.getWeight(), 0.0);
        assertEquals(0.0, s3.getWeight(), 0.0);

        assertEquals(0.0, s4.getWeight(), 0.0);
        assertEquals(23.0, s5.getWeight(), 0.0);
        assertEquals(23.0, s6.getWeight(), 0.0);
        assertEquals(23.0, s7.getWeight(), 0.0);

        assertEquals(0, s4.getElapsedTimeSeconds());
        assertEquals(42, s5.getElapsedTimeSeconds());
        assertEquals(42, s6.getElapsedTimeSeconds());
        assertEquals(42, s7.getElapsedTimeSeconds());
    }

    @Test
    public void testTurnRestriction() {
        StreetEdge e0 = edge(v0, v1, 50.0, StreetTraversalPermission.ALL);
        StreetEdge e1 = edge(v1, v2, 18.4, StreetTraversalPermission.ALL);
        State state = new State(v2, 0, proto.clone());

        state.getOptions().setArriveBy(true);
        _graph.addTurnRestriction(e1, new TurnRestriction(e1, e0, null, TraverseModeSet.allModes()));

        assertNotNull(e0.traverse(e1.traverse(state)));
    }

    /****
     * Private Methods
     ****/

    private IntersectionVertex vertex(String label, double x, double y) {
        IntersectionVertex v = new IntersectionVertex(_graph, label, x, y);
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

}
