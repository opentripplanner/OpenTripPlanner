package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class VehicleParkingEdgeTest {

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
      VehicleParkingSpaces.builder().carSpaces(1).build()
    );

    var s1 = traverse();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void realtimeAvailableCarPlacesFallbackTest() {
    initEdgeAndRequest(StreetMode.CAR_TO_PARK, false, true, null);

    var s1 = traverse();

    assertFalse(State.isEmpty(s1));
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
      VehicleParkingSpaces.builder().bicycleSpaces(1).build()
    );

    var s1 = traverse();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void realtimeAvailableBicyclePlacesFallbackTest() {
    initEdgeAndRequest(StreetMode.BIKE_TO_PARK, true, false, null);

    var s1 = traverse();

    assertFalse(State.isEmpty(s1));
  }

  private void initEdgeAndRequest(
    StreetMode parkingMode,
    boolean bicyclePlaces,
    boolean carPlaces
  ) {
    initEdgeAndRequest(parkingMode, bicyclePlaces, carPlaces, null);
  }

  private void initEdgeAndRequest(
    StreetMode parkingMode,
    boolean bicyclePlaces,
    boolean carPlaces,
    VehicleParkingSpaces availability
  ) {
    var vehicleParking = createVehicleParking(bicyclePlaces, carPlaces, availability);
    this.vertex = new VehicleParkingEntranceVertex(vehicleParking.getEntrances().get(0));

    vehicleParkingEdge = VehicleParkingEdge.createVehicleParkingEdge(vertex);

    this.request = StreetSearchRequest.of().withMode(parkingMode).build();
  }

  private VehicleParking createVehicleParking(
    boolean bicyclePlaces,
    boolean carPlaces,
    VehicleParkingSpaces availability
  ) {
    return StreetModelForTest.vehicleParking()
      .id(TimetableRepositoryForTest.id("VehicleParking"))
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
        .entranceId(TimetableRepositoryForTest.id(id))
        .name(new NonLocalizedString(id))
        .coordinate(new WgsCoordinate(0, 0));
  }

  private State[] traverse() {
    return vehicleParkingEdge.traverse(new State(vertex, request));
  }
}
