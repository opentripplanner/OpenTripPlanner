package org.opentripplanner.routing.algorithm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.StreetVehicleParkingLink;
import org.opentripplanner.routing.edgetype.VehicleParkingEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.VehicleParkingVertex;

import static org.junit.jupiter.api.Assertions.*;

public class VehicleParkingTest {

  private static final String TEST_FEED_ID = "testFeed";

  private Graph graph;
  private StreetVertex A,B;

  @BeforeEach
  public void setUp() throws Exception {
    graph = new Graph();

    A = new IntersectionVertex(graph, "A", 0, 0, "A");
    B = new IntersectionVertex(graph, "B", 1, 0, "B");

    new StreetEdge(A, B, GeometryUtils.makeLineString(A.getX(), A.getY(), B.getX(), B.getY()),
        "AB street", 87, StreetTraversalPermission.ALL, false);
  }

  @Test
  public void bicycleParkingToCarParkingPlaceTest() {
    createVehicleParkingToFirstVertex(false, true);

    AStar aStar = new AStar();

    RoutingRequest request = bicycleParkingRequest();

    ShortestPathTree tree = aStar.getShortestPathTree(request);
    GraphPath path = tree.getPath(B, false);

    assertNull(path);
  }

  @Test
  public void bicycleParkingToCarAndBicycleParkingPlaceTest() {
    createVehicleParkingToFirstVertex(true, true);

    AStar aStar = new AStar();

    RoutingRequest request = bicycleParkingRequest();

    ShortestPathTree tree = aStar.getShortestPathTree(request);
    GraphPath path = tree.getPath(B, false);

    assertParkingPath(path);
  }

  @Test
  public void bicycleParkingToBicycleParkingPlaceTest() {
    createVehicleParkingToFirstVertex(true, false);

    AStar aStar = new AStar();

    RoutingRequest request = bicycleParkingRequest();

    ShortestPathTree tree = aStar.getShortestPathTree(request);
    GraphPath path = tree.getPath(B, false);

    assertParkingPath(path);
  }

  private void createVehicleParkingToFirstVertex(boolean bicyclePlaces, boolean carPlaces) {
    var vehicleParking = VehicleParking.builder()
        .id(new FeedScopedId(TEST_FEED_ID, "Parking"))
        .x(0).y(1)
        .bicyclePlaces(bicyclePlaces)
        .carPlaces(carPlaces)
        .build();

    var parkingVertex = new VehicleParkingVertex(graph, vehicleParking);
    new VehicleParkingEdge(parkingVertex);
    new StreetVehicleParkingLink(A, parkingVertex);
    new StreetVehicleParkingLink(parkingVertex, A);
  }

  private RoutingRequest bicycleParkingRequest() {
    RoutingRequest request = new RoutingRequest(new TraverseModeSet(TraverseMode.WALK, TraverseMode.BICYCLE));
    request.setRoutingContext(graph, A, B);
    request.bikeParkAndRide = true;
    return request;
  }

  private void assertParkingPath(GraphPath path) {
    assertNotNull(path);
    assertEquals(4, path.edges.size());
    assertTrue(path.edges.get(0) instanceof StreetVehicleParkingLink);
    assertTrue(path.edges.get(1) instanceof VehicleParkingEdge);
    assertTrue(path.edges.get(2) instanceof StreetVehicleParkingLink);
    assertTrue(path.edges.get(3) instanceof StreetEdge);
  }
}
