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

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.Coordinate;

import junit.framework.TestCase;

public class TestBikeRental extends TestCase {
    public void testBasic() throws Exception {
        // generate a very simple graph
        Graph graph = new Graph();
        StreetVertex v1 = new IntersectionVertex(graph, "v1", new Coordinate(-77.0492, 38.856),
                "v1");
        StreetVertex v2 = new IntersectionVertex(graph, "v2", new Coordinate(-77.0492, 38.857),
                "v2");
        StreetVertex v3 = new IntersectionVertex(graph, "v3", new Coordinate(-77.0492, 38.858),
                "v3");

        Edge walk = new PlainStreetEdge(v1, v2, GeometryUtils.makeLineString(-77.0492, 38.856,
                -77.0492, 38.857), "S. Crystal Dr", 87, StreetTraversalPermission.PEDESTRIAN, false);

        Edge mustBike = new PlainStreetEdge(v2, v3, GeometryUtils.makeLineString(-77.0492, 38.857,
                -77.0492, 38.858), "S. Crystal Dr", 87, StreetTraversalPermission.BICYCLE, false);

        GenericAStar aStar = new GenericAStar();
        
        // it is impossible to get from v1 to v3 by walking
        TraverseOptions options = new TraverseOptions(new TraverseModeSet("WALK,TRANSIT"));
        options.dateTime = 1000;
        options.setRoutingContext(graph, v1, v3);
        ShortestPathTree tree = aStar.getShortestPathTree(options);

        GraphPath path = tree.getPath(v3, false);
        assertNull(path);

        // or biking
        options = new TraverseOptions(new TraverseModeSet("BICYCLE,TRANSIT"));
        options.dateTime = 1000;
        options.freezeTraverseMode();
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);

        path = tree.getPath(v3, false);
        assertNull(path);

        // or even both (assuming walking bikes is disallowed)
        options = new TraverseOptions(new TraverseModeSet("WALK,BICYCLE,TRANSIT"));
        options.dateTime = 1000;
        options.freezeTraverseMode();
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);

        path = tree.getPath(v3, false);
        assertNull(path);

        // so we add a bike share
        BikeRentalStationVertex station = new BikeRentalStationVertex(graph, "station", -77.049,
                36.856, "station", 10);
        new StreetBikeRentalLink(station, v2);
        new StreetBikeRentalLink(v2, station);
        new RentABikeOnEdge(station, station);
        new RentABikeOffEdge(station, station);
        
        // but we can't get off the bike at v3, so we still fail
        options = new TraverseOptions(new TraverseModeSet("WALK,BICYCLE,TRANSIT"));
        options.dateTime = 1000;
        options.freezeTraverseMode();
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);

        path = tree.getPath(v3, false);
        assertNotNull(path);
        assertFalse(path.states.getLast().isFinal());

        BikeRentalStationVertex station2 = new BikeRentalStationVertex(graph, "station2", -77.049,
                36.857, "station", 10);
        new StreetBikeRentalLink(station2, v3);
        new StreetBikeRentalLink(v3, station2);
        new RentABikeOnEdge(station2, station2);
        new RentABikeOffEdge(station2, station2);
        
        // now we succeed!
        options = new TraverseOptions(new TraverseModeSet("WALK,BICYCLE,TRANSIT"));
        options.dateTime = 1000;
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);

        path = tree.getPath(v3, false);
        assertNotNull(path);
    }
}
