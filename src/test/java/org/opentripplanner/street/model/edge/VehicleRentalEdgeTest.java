package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.service.vehiclerental.street.GeofencingZoneExtension;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class VehicleRentalEdgeTest {

  VehicleRentalEdge vehicleRentalEdge;
  StreetSearchRequest request;
  VehicleRentalPlaceVertex vertex;

  @Test
  void testRentingWithAvailableVehicles() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 3);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testRentingWithNoAvailableVehicles() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 0, 3);

    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void testRentingWithNoAvailableVehiclesAndNoRealtimeUsage() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 0, 3, false, true, false);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testReturningWithAvailableSpaces() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 3);

    var s1 = rentAndDropOff();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testReturningWithNoAvailableSpaces() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 0);

    var s1 = rentAndDropOff();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void testReturningWithNoAvailableSpacesAndOverloading() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 0, true, true, true);

    var s1 = rentAndDropOff();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testReturningWithNoAvailableSpacesAndNoRealtimeUsage() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 0, false, true, false);

    var s1 = rentAndDropOff();

    assertFalse(State.isEmpty(s1));
  }

  @Test
  void testRentingFromClosedStation() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 0, true, false, true);

    var s1 = rent();

    assertTrue(State.isEmpty(s1));
  }

  @Test
  void testReturningToClosedStation() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 3, true, true, true);

    var s1 = rent();

    assertFalse(State.isEmpty(s1));

    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 3, true, false, true);

    var s2 = dropOff(s1[0]);

    assertTrue(State.isEmpty(s2));
  }

  @Test
  void testReturningAndReturningToClosedStationWithNoRealtimeUsage() {
    initEdgeAndRequest(StreetMode.BIKE_RENTAL, 3, 3, false, true, false);

    var s1 = rentAndDropOff();

    assertFalse(State.isEmpty(s1));
  }

  private void initEdgeAndRequest(StreetMode mode, int vehicles, int spaces) {
    initEdgeAndRequest(mode, vehicles, spaces, false, true, true);
  }

  @Nested
  class StartedReverseSearchInNoGeofencingZone {

    private static final String NETWORK = "tier";
    private static final StreetSearchRequest SEARCH_REQUEST = StreetSearchRequest
      .of()
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withArriveBy(true)
      .build();

    private static final VehicleRentalVehicle RENTAL_PLACE = new VehicleRentalVehicle();

    static {
      RENTAL_PLACE.latitude = 1;
      RENTAL_PLACE.longitude = 1;
      RENTAL_PLACE.id = new FeedScopedId(NETWORK, "123");
      RENTAL_PLACE.vehicleType =
        new RentalVehicleType(
          new FeedScopedId(NETWORK, "scooter"),
          "scooter",
          RentalFormFactor.SCOOTER,
          RentalVehicleType.PropulsionType.ELECTRIC,
          100000d
        );
    }

    @Test
    void startedInNoDropOffZone() {
      var rentalVertex = new VehicleRentalPlaceVertex(RENTAL_PLACE);
      var rentalEdge = VehicleRentalEdge.createVehicleRentalEdge(
        rentalVertex,
        RentalFormFactor.SCOOTER
      );

      rentalVertex.addRentalRestriction(noDropOffZone());

      var state = new State(rentalVertex, SEARCH_REQUEST);

      assertEquals(Set.of(NETWORK), state.stateData.noRentalDropOffZonesAtStartOfReverseSearch);

      assertTrue(State.isEmpty(rentalEdge.traverse(state)));
    }

    @Test
    void startedOutsideNoDropOffZone() {
      var rentalVertex = new VehicleRentalPlaceVertex(RENTAL_PLACE);
      var rentalEdge = VehicleRentalEdge.createVehicleRentalEdge(
        rentalVertex,
        RentalFormFactor.SCOOTER
      );
      var state = new State(rentalVertex, SEARCH_REQUEST);

      assertEquals(Set.of(), state.stateData.noRentalDropOffZonesAtStartOfReverseSearch);
      var result = rentalEdge.traverse(state);

      assertEquals(1, result.length);

      var afterTraversal = result[0];
      assertEquals(VehicleRentalState.BEFORE_RENTING, afterTraversal.getVehicleRentalState());
    }

    @Nonnull
    private GeofencingZoneExtension noDropOffZone() {
      return new GeofencingZoneExtension(
        new GeofencingZone(new FeedScopedId(NETWORK, "zone"), null, true, false)
      );
    }
  }

  private void initEdgeAndRequest(
    StreetMode mode,
    int vehicles,
    int spaces,
    boolean overloadingAllowed,
    boolean stationOn,
    boolean useRealtime
  ) {
    var station = TestVehicleRentalStationBuilder
      .of()
      .withVehicles(vehicles)
      .withSpaces(spaces)
      .withOverloadingAllowed(overloadingAllowed)
      .withStationOn(stationOn)
      .build();

    this.vertex = new VehicleRentalPlaceVertex(station);

    vehicleRentalEdge = VehicleRentalEdge.createVehicleRentalEdge(vertex, RentalFormFactor.BICYCLE);

    this.request =
      StreetSearchRequest
        .of()
        .withMode(mode)
        .withPreferences(preferences ->
          preferences
            .withCar(car ->
              car.withRental(rental -> rental.withUseAvailabilityInformation(useRealtime))
            )
            .withBike(bike ->
              bike.withRental(rental -> rental.withUseAvailabilityInformation(useRealtime))
            )
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
