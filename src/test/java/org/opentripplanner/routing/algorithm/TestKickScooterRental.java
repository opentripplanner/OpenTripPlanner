package org.opentripplanner.routing.algorithm;

import junit.framework.TestCase;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.vehicle_sharing.FuelType;
import org.opentripplanner.routing.core.vehicle_sharing.Gearbox;
import org.opentripplanner.routing.core.vehicle_sharing.KickScooterDescription;
import org.opentripplanner.routing.core.vehicle_sharing.Provider;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

public class TestKickScooterRental extends TestCase {

    private static final ParkingZoneInfo EMPTY_PARKING_ZONES = new ParkingZoneInfo();

    public void testIsRoutable() throws Exception {
        // generate a very simple graph
        Graph graph = new Graph();
        StreetVertex v1 = new IntersectionVertex(graph, "v1", -77.0492, 38.856, "v1");
        StreetVertex v2 = new IntersectionVertex(graph, "v2", -77.0492, 38.857, "v2");
        StreetVertex v3 = new IntersectionVertex(graph, "v3", -77.0492, 38.858, "v3");

        @SuppressWarnings("unused")
        Edge pedestrianAndBicycle = new StreetEdge(v1, v2, GeometryUtils.makeLineString(-77.0492, 38.856,
                -77.0492, 38.857), "S. Crystal Dr", 100, StreetTraversalPermission.BICYCLE, false);

        @SuppressWarnings("unused")
        Edge pedestrianOnly = new StreetEdge(v2, v3, GeometryUtils.makeLineString(-77.0492, 38.857,
                -77.0492, 38.858), "S. Crystal Dr", 100, StreetTraversalPermission.PEDESTRIAN, false);

        AStar aStar = new AStar();

        KickScooterDescription scooterDescription = new KickScooterDescription("bleble", -77.0492, 38.858, FuelType.ELECTRIC, Gearbox.AUTOMATIC, null);
        RoutingRequest options = new RoutingRequest(new TraverseModeSet("WALK,CAR,BICYCLE"));
        options.setStartingMode(TraverseMode.WALK);
        options.setRentingAllowed(true);
        options.setRoutingContext(graph, v1, v3);
        ShortestPathTree tree;
        GraphPath path;

        // so we add renting edges
        @SuppressWarnings("unused")
        RentVehicleAnywhereEdge rent1 = new RentVehicleAnywhereEdge(v1, EMPTY_PARKING_ZONES);
        @SuppressWarnings("unused")
        RentVehicleAnywhereEdge rent3 = new RentVehicleAnywhereEdge(v3, EMPTY_PARKING_ZONES);

        // we add a kickscooter
        rent1.getAvailableVehicles().add(scooterDescription);

        // now we succeed!
        options = new RoutingRequest();
        new QualifiedModeSet("CAR,WALK,BICYCLE").applyToRoutingRequest(options);
        options.setStartingMode(TraverseMode.WALK);
        options.setRentingAllowed(true);
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(v3, false);
        assertNotNull(path);
        assertEquals(5, path.states.size());
        assertEquals(scooterDescription, path.states.get(3).getCurrentVehicle());
    }

    public void testSpeeds() throws Exception {
        // generate a very simple graph
        Graph graph = new Graph();
        StreetVertex v1 = new IntersectionVertex(graph, "v1", -77.0492, 38.85, "v1");
        StreetVertex v2 = new IntersectionVertex(graph, "v2", -77.0492, 38.86, "v2");
        StreetVertex v3 = new IntersectionVertex(graph, "v3", -77.0492, 38.87, "v3");

        @SuppressWarnings("unused")
        Edge bicycleAndPedestrianEdge = new StreetEdge(v1, v2, GeometryUtils.makeLineString(-77.0492, 38.85,
                -77.0492, 38.86), "S. Crystal Dr", 100, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, false);

        @SuppressWarnings("unused")
        Edge pedestrianOnly = new StreetEdge(v2, v3, GeometryUtils.makeLineString(-77.0492, 38.86,
                -77.0492, 38.87), "S. Crystal Dr", 100, StreetTraversalPermission.PEDESTRIAN, false);

        AStar aStar = new AStar();

        KickScooterDescription scooterDescription = new KickScooterDescription("bleble", -77.0492, 38.85, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(1,"dime"));
        RoutingRequest options = new RoutingRequest(new TraverseModeSet("WALK,CAR,BICYCLE"));
        options.setStartingMode(TraverseMode.WALK);
        options.setRentingAllowed(true);
        options.setRoutingContext(graph, v1, v3);
        ShortestPathTree tree;
        GraphPath path;

        // so we add renting kickscooter
        @SuppressWarnings("unused")
        RentVehicleAnywhereEdge rent1 = new RentVehicleAnywhereEdge(v1, EMPTY_PARKING_ZONES);
//        Used to droppoff kickscooter
        @SuppressWarnings("unused")
        RentVehicleAnywhereEdge rent3 = new RentVehicleAnywhereEdge(v3, EMPTY_PARKING_ZONES);
        // we add a kickscooter
        rent1.getAvailableVehicles().add(scooterDescription);

        // now we succeed!
        options = new RoutingRequest();
        new QualifiedModeSet("CAR,WALK,BICYCLE").applyToRoutingRequest(options);
        options.setStartingMode(TraverseMode.WALK);
        options.setRentingAllowed(true);
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(v3, false);
        assertNotNull(path);
//        There should be 5 states, 3 for each nodes and two for rental/dropoff.
        assertEquals(5, path.states.size());

        long time1 = path.states.get(2).getTimeInMillis() - path.states.get(1).getTimeInMillis();
        long time2 = path.states.get(3).getTimeInMillis() - path.states.get(2).getTimeInMillis();
//        We should traverse first Edge faster.
        assertTrue(time1 < time2);
    }
}
