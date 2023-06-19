package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model._data.TransitModelForTest;

class VehicleParkingEdgeTest {

  Graph graph;
  VehicleParkingEdge vehicleParkingEdge;
  StreetSearchRequest request;
  VehicleParkingEntranceVertex vertex;

  @Test
  public void availableCarPlacesTest() {
    initEdgeAndRequest(StreetMode.CAR_TO_PARK, false, true);

    var s1 = traverse();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void notAvailableCarPlacesTest() {
    initEdgeAndRequest(StreetMode.CAR_TO_PARK, false, false);

    var s1 = traverse();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  public void realtimeAvailableCarPlacesTest() {
    initEdgeAndRequest(
      StreetMode.CAR_TO_PARK,
      false,
      true,
      VehicleParkingSpaces.builder().carSpaces(1).build(),
      true
    );

    var s1 = traverse();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void realtimeAvailableCarPlacesFallbackTest() {
    initEdgeAndRequest(StreetMode.CAR_TO_PARK, false, true, null, true);

    var s1 = traverse();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void realtimeNotAvailableCarPlacesTest() {
    initEdgeAndRequest(
      StreetMode.CAR_TO_PARK,
      false,
      true,
      VehicleParkingSpaces.builder().carSpaces(0).build(),
      true
    );

    var s1 = traverse();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  public void availableBicyclePlacesTest() {
    initEdgeAndRequest(StreetMode.BIKE_TO_PARK, true, false);

    var s1 = traverse();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void notAvailableBicyclePlacesTest() {
    initEdgeAndRequest(StreetMode.BIKE_TO_PARK, false, false);

    var s1 = traverse();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  public void realtimeAvailableBicyclePlacesTest() {
    initEdgeAndRequest(
      StreetMode.BIKE_TO_PARK,
      true,
      false,
      VehicleParkingSpaces.builder().bicycleSpaces(1).build(),
      true
    );

    var s1 = traverse();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void realtimeAvailableBicyclePlacesFallbackTest() {
    initEdgeAndRequest(StreetMode.BIKE_TO_PARK, true, false, null, true);

    var s1 = traverse();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void realtimeNotAvailableBicyclePlacesTest() {
    initEdgeAndRequest(
      StreetMode.BIKE_TO_PARK,
      true,
      false,
      VehicleParkingSpaces.builder().bicycleSpaces(0).build(),
      true
    );

    var s1 = traverse();

    assertTrue(State.isEmpty(s1));
  }

  private void initEdgeAndRequest(
    StreetMode parkingMode,
    boolean bicyclePlaces,
    boolean carPlaces
  ) {
    initEdgeAndRequest(parkingMode, bicyclePlaces, carPlaces, null, false);
  }

  private void initEdgeAndRequest(
    StreetMode parkingMode,
    boolean bicyclePlaces,
    boolean carPlaces,
    VehicleParkingSpaces availability,
    boolean realtime
  ) {
    graph = new Graph();

    var vehicleParking = createVehicleParking(bicyclePlaces, carPlaces, availability);
    this.vertex = new VehicleParkingEntranceVertex(vehicleParking.getEntrances().get(0));

    vehicleParkingEdge = new VehicleParkingEdge(vertex);

    var parking = new VehicleParkingRequest();
    parking.setUseAvailabilityInformation(realtime);
    this.request = StreetSearchRequest.of().withMode(parkingMode).withParking(parking).build();
  }

  private VehicleParking createVehicleParking(
    boolean bicyclePlaces,
    boolean carPlaces,
    VehicleParkingSpaces availability
  ) {
    return VehicleParking
      .builder()
      .id(TransitModelForTest.id("VehicleParking"))
      .bicyclePlaces(bicyclePlaces)
      .carPlaces(carPlaces)
      .availability(availability)
      .entrances(List.of(vehicleParkingEntrance()))
      .build();
  }

  private VehicleParking.VehicleParkingEntranceCreator vehicleParkingEntrance() {
    String id = "Entrance";
    return builder ->
      builder
        .entranceId(TransitModelForTest.id(id))
        .name(new NonLocalizedString(id))
        .coordinate(new WgsCoordinate(0, 0));
  }

  private State[] traverse() {
    return vehicleParkingEdge.traverse(new State(vertex, request));
  }
}
