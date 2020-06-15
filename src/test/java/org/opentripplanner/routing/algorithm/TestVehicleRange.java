package org.opentripplanner.routing.algorithm;

import junit.framework.TestCase;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.vehicle_sharing.FuelType;
import org.opentripplanner.routing.core.vehicle_sharing.Gearbox;
import org.opentripplanner.routing.core.vehicle_sharing.KickScooterDescription;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

public class TestVehicleRange extends TestCase {
    public void testToFar() {
        Graph graph = new Graph();

        StreetVertex v1 = new IntersectionVertex(graph, "v1", -77.0492, 38.856, "v1");
        StreetVertex v2 = new IntersectionVertex(graph, "v2", -77.0492, 38.857, "v2");
        StreetVertex v3 = new IntersectionVertex(graph, "v3", -77.0492, 38.858, "v3");

        new StreetEdge(v1, v2, GeometryUtils.makeLineString(0, 0, 0, 0), "", 1500, StreetTraversalPermission.BICYCLE, false);
        new StreetEdge(v2, v3, GeometryUtils.makeLineString(0, 0, 0, 0), "s", 2200, StreetTraversalPermission.BICYCLE, false);
//      Vehicle's range is smaller than combine length of the streets. It shouldn't be possible to find route.
        KickScooterDescription kickScooterDescription = new KickScooterDescription("a", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, null, 3 * 1000.);

        RentVehicleAnywhereEdge rentVehicleAnywhereEdge = new RentVehicleAnywhereEdge(v1);
        new RentVehicleAnywhereEdge(v3);
        rentVehicleAnywhereEdge.getAvailableVehicles().add(kickScooterDescription);

        AStar aStar = new AStar();

        RoutingRequest options = new RoutingRequest(new TraverseModeSet("WALK,TRANSIT,BICYCLE"));
        options.setStartingMode(TraverseMode.WALK);
        options.setRentingAllowed(true);
        options.setRoutingContext(graph, v1, v3);
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPath(v3, false);
        assertNull(path);

    }


    public void testInRange() {
        Graph graph = new Graph();

        StreetVertex v1 = new IntersectionVertex(graph, "v1", -77.0492, 38.856, "v1");
        StreetVertex v2 = new IntersectionVertex(graph, "v2", -77.0492, 38.857, "v2");
        StreetVertex v3 = new IntersectionVertex(graph, "v3", -77.0492, 38.858, "v3");
//      Kickscooters range is capped at 4km.
        new StreetEdge(v1, v2, GeometryUtils.makeLineString(0, 0, 0, 0), "", 1500, StreetTraversalPermission.BICYCLE, false);
        new StreetEdge(v2, v3, GeometryUtils.makeLineString(0, 0, 0, 0), "s", 2200, StreetTraversalPermission.BICYCLE, false);
//      Vehicle has enough range to traverse the whole route.
        KickScooterDescription kickScooterDescription = new KickScooterDescription("a", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, null, 3700.);

        RentVehicleAnywhereEdge rentVehicleAnywhereEdge = new RentVehicleAnywhereEdge(v1);
        new RentVehicleAnywhereEdge(v3);
        rentVehicleAnywhereEdge.getAvailableVehicles().add(kickScooterDescription);

        AStar aStar = new AStar();

        RoutingRequest options = new RoutingRequest(new TraverseModeSet("WALK,TRANSIT,BICYCLE"));
        options.setStartingMode(TraverseMode.WALK);
        options.setRentingAllowed(true);
        options.setRoutingContext(graph, v1, v3);
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPath(v3, false);
        assertNotNull(path);
        assertEquals(path.states.size(), 5);

    }
}
