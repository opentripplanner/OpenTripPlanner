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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.NonLocalizedString;

public class TestBikeRental extends TestCase {
    public void testBasic() throws Exception {
        // generate a very simple graph
        Graph graph = new Graph();
        StreetVertex v1 = new IntersectionVertex(graph, "v1", -77.0492, 38.856, "v1");
        StreetVertex v2 = new IntersectionVertex(graph, "v2", -77.0492, 38.857, "v2");
        StreetVertex v3 = new IntersectionVertex(graph, "v3", -77.0492, 38.858, "v3");

        @SuppressWarnings("unused")
        Edge walk = new StreetEdge(v1, v2, GeometryUtils.makeLineString(-77.0492, 38.856,
                -77.0492, 38.857), "S. Crystal Dr", 87, StreetTraversalPermission.PEDESTRIAN, false);

        @SuppressWarnings("unused")
        Edge mustBike = new StreetEdge(v2, v3, GeometryUtils.makeLineString(-77.0492, 38.857,
                -77.0492, 38.858), "S. Crystal Dr", 87, StreetTraversalPermission.BICYCLE, false);

        AStar aStar = new AStar();

        // it is impossible to get from v1 to v3 by walking
        RoutingRequest options = new RoutingRequest(new TraverseModeSet("WALK,TRANSIT"));
        options.setRoutingContext(graph, v1, v3);
        ShortestPathTree tree = aStar.getShortestPathTree(options);

        GraphPath path = tree.getPath(v3, false);
        assertNull(path);

        // or biking + walking (assuming walking bikes is disallowed)
        options = new RoutingRequest(new TraverseModeSet("WALK,BICYCLE,TRANSIT"));
        options.freezeTraverseMode();
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);

        path = tree.getPath(v3, false);
        assertNull(path);

        // so we add a bike share
        BikeRentalStation station = new BikeRentalStation();
        station.id = "id";
        station.name = new NonLocalizedString("station");
        station.x = -77.049;
        station.y = 36.856;
        station.bikesAvailable = 5;
        station.spacesAvailable = 5;
        BikeRentalStationVertex stationVertex = new BikeRentalStationVertex(graph, station);
        new StreetBikeRentalLink(stationVertex, v2);
        new StreetBikeRentalLink(v2, stationVertex);
        Set<String> networks = new HashSet<String>(Arrays.asList("default"));
        new RentABikeOnEdge(stationVertex, stationVertex, networks);
        new RentABikeOffEdge(stationVertex, stationVertex, networks);

        // but we can't get off the bike at v3, so we still fail
        options = new RoutingRequest(new TraverseModeSet("WALK,BICYCLE,TRANSIT"));
        options.freezeTraverseMode();
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);

        path = tree.getPath(v3, false);
        // null is returned because the only state at the target is not final
        assertNull(path);

        BikeRentalStation station2 = new BikeRentalStation();
        station2.id = "id2";
        station2.name = new NonLocalizedString("station2");
        station2.x = -77.049;
        station2.y = 36.857;
        station2.bikesAvailable = 5;
        station2.spacesAvailable = 5;
        BikeRentalStationVertex stationVertex2 = new BikeRentalStationVertex(graph, station2);
        new StreetBikeRentalLink(stationVertex2, v3);
        new StreetBikeRentalLink(v3, stationVertex2);
        new RentABikeOnEdge(stationVertex2, stationVertex2, networks);
        new RentABikeOffEdge(stationVertex2, stationVertex2, networks);

        // now we succeed!
        options = new RoutingRequest();
        new QualifiedModeSet("BICYCLE_RENT,TRANSIT").applyToRoutingRequest(options);
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);

        path = tree.getPath(v3, false);
        assertNotNull(path);
    }
}
