package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;
import org.opentripplanner.util.NonLocalizedString;

class VehicleParkingEdgeTest extends GraphRoutingTest {

  Graph graph;
  VehicleParkingEdge vehicleParkingEdge;
  RoutingRequest routingRequest;

  @Test
  public void availableCarPlacesTest() {
    initEdgeAndRequest(TraverseMode.CAR, false, true);

    State s0 = new State(routingRequest);

    State s1 = vehicleParkingEdge.traverse(s0);

    assertNotNull(s1);
  }

  @Test
  public void notAvailableCarPlacesTest() {
    initEdgeAndRequest(TraverseMode.CAR, false, false);

    State s0 = new State(routingRequest);

    State s1 = vehicleParkingEdge.traverse(s0);

    assertNull(s1);
  }

  @Test
  public void realtimeAvailableCarPlacesTest() {
    initEdgeAndRequest(TraverseMode.CAR, false, true, VehicleParkingSpaces.builder().carSpaces(1).build(), true);

    State s0 = new State(routingRequest);

    State s1 = vehicleParkingEdge.traverse(s0);

    assertNotNull(s1);
  }

  @Test
  public void realtimeAvailableCarPlacesFallbackTest() {
    initEdgeAndRequest(TraverseMode.CAR, false, true, null, true);

    State s0 = new State(routingRequest);

    State s1 = vehicleParkingEdge.traverse(s0);

    assertNotNull(s1);
  }

  @Test
  public void realtimeNotAvailableCarPlacesTest() {
    initEdgeAndRequest(TraverseMode.CAR, false, true, VehicleParkingSpaces.builder().carSpaces(0).build(), true);

    State s0 = new State(routingRequest);

    State s1 = vehicleParkingEdge.traverse(s0);

    assertNull(s1);
  }

  @Test
  public void availableBicyclePlacesTest() {
    initEdgeAndRequest(TraverseMode.BICYCLE, true, false);

    State s0 = new State(routingRequest);

    State s1 = vehicleParkingEdge.traverse(s0);

    assertNotNull(s1);
  }

  @Test
  public void notAvailableBicyclePlacesTest() {
    initEdgeAndRequest(TraverseMode.BICYCLE, false, false);

    State s0 = new State(routingRequest);

    State s1 = vehicleParkingEdge.traverse(s0);

    assertNull(s1);
  }

  @Test
  public void realtimeAvailableBicyclePlacesTest() {
    initEdgeAndRequest(TraverseMode.BICYCLE, true, false, VehicleParkingSpaces.builder().bicycleSpaces(1).build(), true);

    State s0 = new State(routingRequest);

    State s1 = vehicleParkingEdge.traverse(s0);

    assertNotNull(s1);
  }

  @Test
  public void realtimeAvailableBicyclePlacesFallbackTest() {
    initEdgeAndRequest(TraverseMode.BICYCLE, true, false, null, true);

    State s0 = new State(routingRequest);

    State s1 = vehicleParkingEdge.traverse(s0);

    assertNotNull(s1);
  }

  @Test
  public void realtimeNotAvailableBicyclePlacesTest() {
    initEdgeAndRequest(TraverseMode.BICYCLE, true, false, VehicleParkingSpaces.builder().bicycleSpaces(0).build(), true);

    State s0 = new State(routingRequest);

    State s1 = vehicleParkingEdge.traverse(s0);

    assertNull(s1);
  }

  private void initEdgeAndRequest(TraverseMode parkingMode, boolean bicyclePlaces, boolean carPlaces) {
    initEdgeAndRequest(parkingMode, bicyclePlaces, carPlaces, null, false);
  }

  private void initEdgeAndRequest(TraverseMode parkingMode, boolean bicyclePlaces, boolean carPlaces, VehicleParkingSpaces availability, boolean realtime) {
    graph = new Graph();

    var vehicleParking = createVehicleParking(bicyclePlaces, carPlaces, availability);
    var vertex = new VehicleParkingEntranceVertex(graph, vehicleParking.getEntrances().get(0));

    vehicleParkingEdge = new VehicleParkingEdge(vertex);

    routingRequest = new RoutingRequest();
    routingRequest.setRoutingContext(graph, vertex, vertex);
    routingRequest.parkAndRide = true;
    routingRequest.useVehicleParkingAvailabilityInformation = realtime;
    routingRequest.streetSubRequestModes = new TraverseModeSet(TraverseMode.WALK, parkingMode);
  }

  private VehicleParking createVehicleParking(boolean bicyclePlaces, boolean carPlaces, VehicleParkingSpaces availability) {
    return VehicleParking.builder()
        .id(new FeedScopedId(TEST_FEED_ID, "VehicleParking"))
        .bicyclePlaces(bicyclePlaces)
        .carPlaces(carPlaces)
        .availability(availability)
        .entrances(
            List.of(vehicleParkingEntrance())
        )
        .build();
  }

  private VehicleParking.VehicleParkingEntranceCreator vehicleParkingEntrance() {
    String id = "Entrance";
    return builder -> builder
        .entranceId(new FeedScopedId(TEST_FEED_ID, id))
        .name(new NonLocalizedString(id))
        .x(0)
        .y(0);
  }

}
