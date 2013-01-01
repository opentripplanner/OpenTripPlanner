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

package org.opentripplanner.routing.algorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.algorithm.strategies.MultiTargetTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SimpleConcreteEdge;
import org.opentripplanner.routing.graph.SimpleConcreteVertex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class TurnRestrictionTest {

    private Graph _graph;

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

        // Turn restrictions
        DisallowTurn(maple_main1, main1_2);
        DisallowTurn(maple_main2, main2_3);
        DisallowTurn(main_broad2, broad2_3);
    }

    @Test
    public void testForward() {
        Vertex origin = _graph.getVertex("maple_1st");
        Vertex destination = _graph.getVertex("broad_3rd");
        assertNotNull(origin);
        assertNotNull(destination);

        RoutingRequest options = new RoutingRequest();
        options.setWalkSpeed(1.0);

        options.setRoutingContext(_graph, origin, destination);
        ShortestPathTree tree = new GenericAStar().getShortestPathTree(options);

        GraphPath path = tree.getPath(destination, false);
        assertNotNull(path);
        
        // If not for turn restrictions, the shortest path would be to take 1st to Main,
        // Main to 2nd, 2nd to Broad and Broad until the corner of Broad and 3rd. 
        // However, most of these turns are not allowed. Instead, the shortest allowed
        // path is 1st to Broad, Broad to 3rd.

         List<State> states = path.states;
         assertEquals(5, states.size());
         
         assertEquals("maple_1st", states.get(0).getVertex().getLabel()); 
         assertEquals("main_1st", states.get(1).getVertex().getLabel()); 
         assertEquals("broad_1st", states.get(2).getVertex().getLabel()); 
         assertEquals("broad_2nd", states.get(3).getVertex().getLabel()); 
         assertEquals("broad_3rd", states.get(4).getVertex().getLabel());
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

    private void DisallowTurn(PlainStreetEdge from, PlainStreetEdge to) {
        TurnRestrictionType rType = TurnRestrictionType.NO_TURN;
        TurnRestriction restrict = new TurnRestriction(from, to, rType, TraverseModeSet.allModes());
        from.addTurnRestriction(restrict);
    }

}
