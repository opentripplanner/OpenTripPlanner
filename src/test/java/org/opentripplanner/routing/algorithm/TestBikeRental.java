package org.opentripplanner.routing.algorithm;

import junit.framework.TestCase;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.NonLocalizedString;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
        RoutingRequest options = new RoutingRequest(new TraverseModeSet(TraverseMode.WALK,TraverseMode.TRANSIT));
        options.setRoutingContext(graph, v1, v3);
        ShortestPathTree tree = aStar.getShortestPathTree(options);

        GraphPath path = tree.getPath(v3, false);
        assertNull(path);

        // or biking + walking (assuming walking bikes is disallowed)
        options = new RoutingRequest(new TraverseModeSet(TraverseMode.WALK,TraverseMode.BICYCLE,TraverseMode.TRANSIT));
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
        options = new RoutingRequest(new TraverseModeSet(TraverseMode.WALK,TraverseMode.BICYCLE,TraverseMode.TRANSIT));
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
        options.streetSubRequestModes.setWalk(true);
        options.streetSubRequestModes.setBicycle(true);
        options.bikeRental = true;
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);

        path = tree.getPath(v3, false);
        assertNotNull(path);
    }
}
