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

package org.opentripplanner.routing.algorithm;

import junit.framework.TestCase;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.algorithm.strategies.InterleavedBidirectionalHeuristic;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.NonLocalizedString;

/**
 * Test P+R (both car P+R and bike P+R).
 * 
 * @author laurent
 */
public class TestParkAndRide extends TestCase {

    private Graph graph;
    private StreetVertex A,B,C,D;

    @Override
    protected void setUp() throws Exception {
        graph = new Graph();

        // Generate a very simple graph
        A = new IntersectionVertex(graph, "A", 0.000, 45, "A");
        B = new IntersectionVertex(graph, "B", 0.001, 45, "B");
        C = new IntersectionVertex(graph, "C", 0.002, 45, "C");
        D = new IntersectionVertex(graph, "D", 0.003, 45, "D");

        @SuppressWarnings("unused")
        Edge driveOnly = new StreetEdge(A, B, GeometryUtils.makeLineString(0.000, 45, 0.001, 45),
                "AB street", 87, StreetTraversalPermission.CAR, false);

        @SuppressWarnings("unused")
        Edge walkAndBike = new StreetEdge(B, C, GeometryUtils.makeLineString(0.001, 45, 0.002,
                45), "BC street", 87, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, false);

        @SuppressWarnings("unused")
        Edge walkOnly = new StreetEdge(C, D, GeometryUtils.makeLineString(0.002, 45, 0.003,
                45), "CD street", 87, StreetTraversalPermission.PEDESTRIAN, false);
    };
    
    public void testCar() throws Exception {

        AStar aStar = new AStar();

        // It is impossible to get from A to C in WALK mode,
        RoutingRequest options = new RoutingRequest(new TraverseModeSet("WALK"));
        options.setRoutingContext(graph, A, C);
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPath(C, false);
        assertNull(path);

        // or CAR+WALK (no P+R).
        options = new RoutingRequest("WALK,CAR");
        options.freezeTraverseMode();
        options.setRoutingContext(graph, A, C);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(C, false);
        assertNull(path);

        // So we Add a P+R at B.
        ParkAndRideVertex PRB = new ParkAndRideVertex(graph, "P+R", "P+R.B", 0.001, 45.00001,
                new NonLocalizedString("P+R B"));
        new ParkAndRideEdge(PRB);
        new ParkAndRideLinkEdge(PRB, B);
        new ParkAndRideLinkEdge(B, PRB);

        // But it is still impossible to get from A to C by WALK only
        // (AB is CAR only).
        options = new RoutingRequest("WALK");
        options.freezeTraverseMode();
        options.setRoutingContext(graph, A, C);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(C, false);
        assertNull(path);
        
        // Or CAR only (BC is WALK only).
        options = new RoutingRequest("CAR");
        options.freezeTraverseMode();
        options.setRoutingContext(graph, A, C);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(C, false);
        assertNull(path);

        // But we can go from A to C with CAR+WALK mode using P+R. arriveBy false
        options = new RoutingRequest("WALK,CAR_PARK,TRANSIT");
        //options.arriveBy
        options.setRoutingContext(graph, A, C);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(C, false);
        assertNotNull(path);

        // But we can go from A to C with CAR+WALK mode using P+R. arriveBy true
        options = new RoutingRequest("WALK,CAR_PARK,TRANSIT");
        options.setArriveBy(true);
        options.setRoutingContext(graph, A, C);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(A, false);
        assertNotNull(path);


        // But we can go from A to C with CAR+WALK mode using P+R. arriveBy true interleavedBidiHeuristic
        options = new RoutingRequest("WALK,CAR_PARK,TRANSIT");
        options.setArriveBy(true);
        options.setRoutingContext(graph, A, C);
        options.rctx.remainingWeightHeuristic = new InterleavedBidirectionalHeuristic();
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(A, false);
        assertNotNull(path);

        // But we can go from A to C with CAR+WALK mode using P+R. arriveBy false interleavedBidiHeuristic
        options = new RoutingRequest("WALK,CAR_PARK,TRANSIT");
        //options.arriveBy
        options.setRoutingContext(graph, A, C);
        options.rctx.remainingWeightHeuristic = new InterleavedBidirectionalHeuristic();
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(C, false);
        assertNotNull(path);
    }

    public void testBike() throws Exception {

        AStar aStar = new AStar();

        // Impossible to get from B to D in BIKE+WALK (no bike P+R).
        RoutingRequest options = new RoutingRequest("BICYCLE_PARK,TRANSIT");
        options.freezeTraverseMode();
        options.setRoutingContext(graph, B, D);
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPath(D, false);
        assertNull(path);

        // So we add a bike P+R at C.
        BikePark bpc = new BikePark();
        bpc.id = "bpc";
        bpc.name = "Bike Park C";
        bpc.x = 0.002;
        bpc.y = 45.00001;
        bpc.spacesAvailable = 1;
        BikeParkVertex BPRC = new BikeParkVertex(graph, bpc);
        new BikeParkEdge(BPRC);
        new StreetBikeParkLink(BPRC, C);
        new StreetBikeParkLink(C, BPRC);

        // Still impossible from B to D by bike only (CD is WALK only).
        options = new RoutingRequest("BICYCLE");
        options.setRoutingContext(graph, B, D);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(D, false);
        assertNotNull(path);
        State s = tree.getState(D);
        assertFalse(s.isBikeParked());
        // TODO backWalkingBike flag is broken
        // assertTrue(s.isBackWalkingBike());
        assertTrue(s.getBackMode() == TraverseMode.WALK);

        // But we can go from B to D using bike P+R.
        options = new RoutingRequest("BICYCLE_PARK,WALK,TRANSIT");
        options.setRoutingContext(graph, B, D);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(D, false);
        assertNotNull(path);
        s = tree.getState(D);
        assertTrue(s.isBikeParked());
        assertFalse(s.isBackWalkingBike());
    }
}
