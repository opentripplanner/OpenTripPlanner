package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.service.vehiclerental.model.RentalVehicleType.PropulsionType.ELECTRIC;
import static org.opentripplanner.service.vehiclerental.model.RentalVehicleType.PropulsionType.HUMAN;
import static org.opentripplanner.street.model.RentalFormFactor.BICYCLE;
import static org.opentripplanner.street.model.RentalFormFactor.CAR;
import static org.opentripplanner.street.model.RentalFormFactor.MOPED;
import static org.opentripplanner.street.model.RentalFormFactor.SCOOTER;
import static org.opentripplanner.street.model.StreetMode.BIKE_RENTAL;
import static org.opentripplanner.street.model.StreetMode.CAR_RENTAL;
import static org.opentripplanner.street.model.StreetMode.SCOOTER_RENTAL;
import static org.opentripplanner.street.search.state.VehicleRentalState.HAVE_RENTED;
import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.TestFreeFloatingRentalVehicleBuilder;
import org.opentripplanner.service.vehiclerental.model.TestGeofencingZoneBuilder;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.service.vehiclerental.street.geofencing.GeofencingBoundaryExtension;
import org.opentripplanner.street.geometry.Polygons;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.search.request.RentalPeriod;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestBuilder;
import org.opentripplanner.street.search.state.State;

class VehicleRentalEdgeTest {

  VehicleRentalEdge vehicleRentalEdge;
  StreetSearchRequest request;
  VehicleRentalPlaceVertex vertex;

  @Test
  void testBicycleMopedRental() {
    initEdgeAndRequest(BIKE_RENTAL, MOPED, ELECTRIC, 3, 3, false, true, true, false);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testScooterBicycleRental() {
    initEdgeAndRequest(SCOOTER_RENTAL, BICYCLE, HUMAN, 3, 3, false, true, true, false);

    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void testRentingWithAvailableBikes() {
    initBicycleEdgeAndRequest(3, 3);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testRentingWithNoAvailableVehicles() {
    initBicycleEdgeAndRequest(0, 3);

    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void testRentingWithNoAvailableVehiclesAndNoRealtimeUsage() {
    initEdgeAndRequest(BIKE_RENTAL, BICYCLE, HUMAN, 0, 3, false, true, false, false);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testReturningWithAvailableSpaces() {
    initBicycleEdgeAndRequest(3, 3);

    var s1 = rentAndDropOff();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testReturningWithNoAvailableSpaces() {
    initBicycleEdgeAndRequest(3, 0);

    var s1 = rentAndDropOff();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void testReturningWithNoAvailableSpacesAndOverloading() {
    initBicycleEdgeAndRequest(3, 0, true, true, true, false);

    var s1 = rentAndDropOff();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testReturningWithNoAvailableSpacesAndNoRealtimeUsage() {
    initBicycleEdgeAndRequest(3, 0, false, true, false, false);

    var s1 = rentAndDropOff();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testRentingFromClosedStation() {
    initBicycleEdgeAndRequest(3, 0, true, false, true, false);

    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void testReturningAndReturningToClosedStationWithNoRealtimeUsage() {
    initBicycleEdgeAndRequest(3, 3, false, true, false, false);

    var s1 = rentAndDropOff();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testRentingWithFreeFloatingBicycle() {
    initFreeFloatingEdgeAndRequest(BIKE_RENTAL, BICYCLE, false);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testRentingWithFreeFloatingScooter() {
    initFreeFloatingEdgeAndRequest(SCOOTER_RENTAL, SCOOTER, false);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testRentingWithFreeFloatingCar() {
    initFreeFloatingEdgeAndRequest(CAR_RENTAL, CAR, false);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testBannedBicycleNetworkStation() {
    initEdgeAndRequest(BIKE_RENTAL, BICYCLE, HUMAN, 3, 3, false, true, true, true);

    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void testBannedBicycleNetworkFreeFloating() {
    initFreeFloatingEdgeAndRequest(BIKE_RENTAL, BICYCLE, true);

    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void testBannedScooterNetworkFreeFloating() {
    initFreeFloatingEdgeAndRequest(SCOOTER_RENTAL, SCOOTER, true);

    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void testBannedCarNetworkFreeFloating() {
    initFreeFloatingEdgeAndRequest(CAR_RENTAL, CAR, true);

    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void propulsionTypeIsStoredInStateAfterRentingFromStation() {
    initEdgeAndRequest(BIKE_RENTAL, BICYCLE, ELECTRIC, 3, 3, false, true, true, false);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));
    assertEquals(ELECTRIC, s1[0].rentalVehiclePropulsionType());
    assertTrue(s1[0].isRentingVehicle());
  }

  @Test
  void propulsionTypeIsStoredInStateAfterRentingFloatingVehicle() {
    initFreeFloatingEdgeAndRequest(SCOOTER_RENTAL, SCOOTER, false);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));
    // Free floating vehicles from TestFreeFloatingRentalVehicleBuilder use ELECTRIC propulsion
    assertEquals(ELECTRIC, s1[0].rentalVehiclePropulsionType());
    assertTrue(s1[0].isRentingVehicle());
  }

  @Test
  void testWithFreeFloatingVehicleWithoutRequiredAvailability() {
    var tenMinutesLater = Instant.now().plus(10, ChronoUnit.MINUTES);
    initFreeFloatingEdgeAndRequestForAvailability(tenMinutesLater, Duration.ofMinutes(15), false);
    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void testWithFreeFloatingVehicleAndArrivalWithoutRequiredAvailability() {
    var tenMinutesLater = Instant.now().minus(10, ChronoUnit.MINUTES);
    initFreeFloatingEdgeAndRequestForAvailability(tenMinutesLater, Duration.ofMinutes(1), true);
    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void testWithFreeFloatingVehicleWithRequiredAvailabilityInformation() {
    var tenMinutesLater = Instant.now().plus(10, ChronoUnit.MINUTES);
    initFreeFloatingEdgeAndRequestForAvailability(tenMinutesLater, Duration.ofMinutes(5), false);
    var s1 = rent();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testWithFreeFloatingVehicleAndArrivalWithRequiredAvailabilityInformation() {
    var tenMinutesLater = Instant.now().plus(1, ChronoUnit.MINUTES);
    initFreeFloatingEdgeAndRequestForAvailability(tenMinutesLater, Duration.ofMinutes(1), true);
    var s1 = rent();

    assertFalse(State.isEmpty(s1));
  }

  /**
   * When a generic RENTING_FLOATING state (network=null) has a network in committedNetworks,
   * VehicleRentalEdge should block picking up a vehicle of that network.
   */
  @Test
  void committedNetworkBlocksGenericPickup() {
    initFreeFloatingEdgeAndRequest(SCOOTER_RENTAL, SCOOTER, false);

    var arriveByReq = StreetSearchRequest.copyOf(request).withArriveBy(true).build();
    var editor = new org.opentripplanner.street.search.state.StateEditor(vertex, arriveByReq);
    editor.dropFloatingVehicle(SCOOTER, ELECTRIC, null, true);
    editor.addCommittedNetwork(vertex.getStation().network());
    var genericState = editor.makeState();

    assertTrue(genericState.getCommittedNetworks().contains(vertex.getStation().network()));

    var result = vehicleRentalEdge.traverse(genericState);
    assertTrue(State.isEmpty(result));
  }

  /**
   * When a generic RENTING_FLOATING state does NOT have the network committed,
   * VehicleRentalEdge should allow picking up the vehicle.
   */
  @Test
  void uncommittedNetworkAllowsGenericPickup() {
    initFreeFloatingEdgeAndRequest(SCOOTER_RENTAL, SCOOTER, false);

    var arriveByReq = StreetSearchRequest.copyOf(request).withArriveBy(true).build();
    var editor = new org.opentripplanner.street.search.state.StateEditor(vertex, arriveByReq);
    editor.dropFloatingVehicle(SCOOTER, ELECTRIC, null, true);
    editor.addCommittedNetwork("some-other-network");
    var genericState = editor.makeState();

    assertFalse(genericState.getCommittedNetworks().contains(vertex.getStation().network()));

    var result = vehicleRentalEdge.traverse(genericState);
    assertFalse(State.isEmpty(result));
  }

  /**
   * When a floating vehicle is at a no-traversal zone boundary vertex, pickup should produce
   * both RENTING_FLOATING (for riding away from zone) and HAVE_RENTED (for walking into zone).
   */
  @Test
  void pickupAtNoTraversalBoundaryVertexShouldFork() {
    initFreeFloatingEdgeAndRequest(SCOOTER_RENTAL, SCOOTER, false);

    var noTraversalZone = TestGeofencingZoneBuilder.of(
      TestFreeFloatingRentalVehicleBuilder.NETWORK_1,
      "no-traverse"
    )
      .withGeometry(Polygons.OSLO)
      .noTraversal()
      .build();
    vertex.addGeofencingBoundary(new GeofencingBoundaryExtension(noTraversalZone, true));

    var result = rent();

    assertEquals(2, result.length);

    var renting = Arrays.stream(result)
      .filter(s -> s.getVehicleRentalState() == RENTING_FLOATING)
      .findFirst();
    assertTrue(renting.isPresent(), "should have a RENTING_FLOATING branch");

    var dropped = Arrays.stream(result)
      .filter(s -> s.getVehicleRentalState() == HAVE_RENTED)
      .findFirst();
    assertTrue(dropped.isPresent(), "should have a HAVE_RENTED branch for walking into zone");
  }

  private void initBicycleEdgeAndRequest(int vehicles, int spaces) {
    initEdgeAndRequest(BIKE_RENTAL, BICYCLE, HUMAN, vehicles, spaces, false, true, true, false);
  }

  private void initBicycleEdgeAndRequest(
    int vehicles,
    int spaces,
    boolean overloadingAllowed,
    boolean stationOn,
    boolean useRealtime,
    boolean banNetwork
  ) {
    initEdgeAndRequest(
      BIKE_RENTAL,
      BICYCLE,
      HUMAN,
      vehicles,
      spaces,
      overloadingAllowed,
      stationOn,
      useRealtime,
      banNetwork
    );
  }

  private void initEdgeAndRequest(
    StreetMode mode,
    RentalFormFactor formFactor,
    RentalVehicleType.PropulsionType propulsionType,
    int vehicles,
    int spaces,
    boolean overloadingAllowed,
    boolean stationOn,
    boolean useRealtime,
    boolean banNetwork
  ) {
    var station = TestVehicleRentalStationBuilder.of()
      .withVehicleType(formFactor, propulsionType, vehicles, spaces)
      .withOverloadingAllowed(overloadingAllowed)
      .withStationOn(stationOn)
      .build();

    this.vertex = new VehicleRentalPlaceVertex(station);

    vehicleRentalEdge = VehicleRentalEdge.createVehicleRentalEdge(vertex, formFactor);

    Set<String> bannedNetworks = banNetwork ? Set.of(station.network()) : Set.of();

    this.request = StreetSearchRequest.of()
      .withMode(mode)
      .withBike(bike ->
        bike.withRental(rental ->
          rental.withUseAvailabilityInformation(useRealtime).withBannedNetworks(bannedNetworks)
        )
      )
      .build();
  }

  private void initFreeFloatingEdgeAndRequest(
    StreetMode mode,
    RentalFormFactor formFactor,
    boolean banNetwork
  ) {
    this.vertex = StreetModelFactory.rentalVertex(formFactor);

    vehicleRentalEdge = VehicleRentalEdge.createVehicleRentalEdge(vertex, formFactor);

    Set<String> bannedNetworks = banNetwork ? Set.of(this.vertex.getStation().network()) : Set.of();

    this.request = StreetSearchRequest.of()
      .withMode(mode)
      .withCar(car -> car.withRental(rental -> rental.withBannedNetworks(bannedNetworks)))
      .withBike(bike -> bike.withRental(rental -> rental.withBannedNetworks(bannedNetworks)))
      .withScooter(scooter ->
        scooter.withRental(rental -> rental.withBannedNetworks(bannedNetworks))
      )
      .build();
  }

  private void initFreeFloatingEdgeAndRequestForAvailability(
    Instant vehicleAvailableUntil,
    Duration rentalDuration,
    boolean arriveBy
  ) {
    RentalFormFactor formFactor = RentalFormFactor.CAR;
    this.vertex = StreetModelFactory.rentalVertex(formFactor, vehicleAvailableUntil);
    this.vehicleRentalEdge = VehicleRentalEdge.createVehicleRentalEdge(vertex, formFactor);
    StreetSearchRequestBuilder streetSearchRequestBuilder = StreetSearchRequest.of()
      .withMode(CAR_RENTAL)
      .withArriveBy(arriveBy);
    if (rentalDuration != null) {
      var now = Instant.now();
      var rentalPeriod = arriveBy
        ? RentalPeriod.createFromLatestArrivalTime(now, rentalDuration)
        : RentalPeriod.createFromEarliestDepartureTime(now, rentalDuration);
      streetSearchRequestBuilder.withRentalPeriod(rentalPeriod);
    }
    this.request = streetSearchRequestBuilder.build();
  }

  private State[] rent() {
    return vehicleRentalEdge.traverse(new State(vertex, request));
  }

  private State[] rentAndDropOff() {
    var s0 = singleState(vehicleRentalEdge.traverse(new State(vertex, request)));
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
