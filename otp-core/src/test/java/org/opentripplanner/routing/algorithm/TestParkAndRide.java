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
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.ParkAndRideEdge;
import org.opentripplanner.routing.edgetype.ParkAndRideLinkEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

/**
 * Test P+R.
 * 
 * @author laurent
 */
public class TestParkAndRide extends TestCase {

    public void testBasic() throws Exception {

        // Generate a very simple graph
        Graph graph = new Graph();
        StreetVertex A = new IntersectionVertex(graph, "A", 0.000, 45, "A");
        StreetVertex B = new IntersectionVertex(graph, "B", 0.001, 45, "B");
        StreetVertex C = new IntersectionVertex(graph, "C", 0.002, 45, "C");

        @SuppressWarnings("unused")
        Edge walk = new PlainStreetEdge(A, B, GeometryUtils.makeLineString(0.000, 45, 0.001, 45),
                "AB street", 87, StreetTraversalPermission.CAR, false);

        @SuppressWarnings("unused")
        Edge mustDrive = new PlainStreetEdge(B, C, GeometryUtils.makeLineString(0.001, 45, 0.002,
                45), "BC street", 87, StreetTraversalPermission.PEDESTRIAN, false);

        GenericAStar aStar = new GenericAStar();

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
                "P+R B");
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

        // But we can go from A to C with CAR+WALK mode using P+R.
        options = new RoutingRequest("WALK,CAR_PARK,TRANSIT");
        options.setRoutingContext(graph, A, C);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(C, false);
        assertNotNull(path);
    }
}
