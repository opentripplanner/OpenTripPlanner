package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.VehicleRentalRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

class VehicleRentalEdgeTest {

  Graph graph;
  VehicleRentalEdge vehicleRentalEdge;
  StreetSearchRequest request;
  VehicleRentalPlaceVertex vertex;

  @Test
  public void testRentingWithAvailableVehicles() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 3);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void testRentingWithNoAvailableVehicles() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 0, 3);

    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  public void testRentingWithNoAvailableVehiclesAndNoRealtimeUsage() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 0, 3, false, true, false);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void testReturningWithAvailableSpaces() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 3);

    var s1 = rentAndDropOff();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void testReturningWithNoAvailableSpaces() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 0);

    var s1 = rentAndDropOff();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  public void testReturningWithNoAvailableSpacesAndOverloading() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 0, true, true, true);

    var s1 = rentAndDropOff();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void testReturningWithNoAvailableSpacesAndNoRealtimeUsage() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 0, false, true, false);

    var s1 = rentAndDropOff();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  public void testRentingFromClosedStation() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 0, true, false, true);

    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  public void testReturningToClosedStation() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 3, true, true, true);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));

    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 3, true, false, true);

    var s2 = dropOff(s1[0]);

    assertTrue(State.isEmpty(s2));
  }

  @Test
  public void testReturningAndReturningToClosedStationWithNoRealtimeUsage() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 3, false, true, false);

    var s1 = rentAndDropOff();

    assertFalse(State.isEmpty(s1));
  }

  private void initEdgeAndRequest(StreetMode mode, int vehicles, int spaces) {
    initEdgeAndRequest(mode, vehicles, spaces, false, true, true);
  }

  private void initEdgeAndRequest(
    StreetMode mode,
    int vehicles,
    int spaces,
    boolean overloadingAllowed,
    boolean stationOn,
    boolean useRealtime
  ) {
    graph = new Graph();

    var station = TestVehicleRentalStationBuilder
      .of()
      .withVehicles(vehicles)
      .withSpaces(spaces)
      .withOverloadingAllowed(overloadingAllowed)
      .withStationOn(stationOn)
      .build();

    this.vertex = new VehicleRentalPlaceVertex(graph, station);

    vehicleRentalEdge = new VehicleRentalEdge(vertex, RentalFormFactor.BICYCLE);

    var rentalRequest = new VehicleRentalRequest();
    this.request =
      StreetSearchRequest
        .of()
        .withMode(mode)
        .withRental(rentalRequest)
        .withPreferences(preferences ->
          preferences
            .withRental(rental -> rental.withUseAvailabilityInformation(useRealtime).build())
            .build()
        )
        .build();
  }

  private State[] rent() {
    return vehicleRentalEdge.traverse(new State(vertex, request));
  }

  private State[] rentAndDropOff() {
    var s0 = singleState(vehicleRentalEdge.traverse(new State(vertex, request)));
    return vehicleRentalEdge.traverse(s0);
  }

  private State[] dropOff(State s0) {
    return vehicleRentalEdge.traverse(s0);
  }

  private static State singleState(State[] resultingStates) {
    if (resultingStates.length == 1) {
      return resultingStates[0];
    } else {
      fail("Expected a single state from traverse() method but received " + resultingStates.length);
      return null;
    }
  }
}
