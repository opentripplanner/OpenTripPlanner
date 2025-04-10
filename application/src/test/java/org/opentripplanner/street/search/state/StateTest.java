package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_TO_PARK;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_HAILING;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_PICKUP;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_TO_PARK;
import static org.opentripplanner.routing.api.request.StreetMode.FLEXIBLE;
import static org.opentripplanner.routing.api.request.StreetMode.SCOOTER_RENTAL;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.search.TraverseMode.BICYCLE;
import static org.opentripplanner.street.search.TraverseMode.CAR;
import static org.opentripplanner.street.search.TraverseMode.SCOOTER;
import static org.opentripplanner.street.search.TraverseMode.WALK;
import static org.opentripplanner.street.search.state.TestStateBuilder.ofCarRental;
import static org.opentripplanner.street.search.state.TestStateBuilder.ofDriving;
import static org.opentripplanner.street.search.state.TestStateBuilder.ofWalking;
import static org.opentripplanner.street.search.state.VehicleRentalState.BEFORE_RENTING;
import static org.opentripplanner.street.search.state.VehicleRentalState.HAVE_RENTED;
import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class StateTest {

  Vertex v1 = intersectionVertex(1, 1);

  static Set<VehicleRentalState> NULL_RENTAL_STATES;

  static {
    NULL_RENTAL_STATES = new HashSet<>();
    NULL_RENTAL_STATES.add(null);
  }

  static Stream<Arguments> testCases() {
    return Stream.of(
      of(SCOOTER_RENTAL, false, Set.of(BEFORE_RENTING), Set.of(WALK)),
      of(SCOOTER_RENTAL, true, Set.of(HAVE_RENTED, RENTING_FLOATING), Set.of(WALK, SCOOTER)),
      of(BIKE_RENTAL, false, Set.of(BEFORE_RENTING), Set.of(WALK)),
      of(BIKE_RENTAL, true, Set.of(HAVE_RENTED, RENTING_FLOATING), Set.of(WALK, BICYCLE)),
      of(CAR_RENTAL, false, Set.of(BEFORE_RENTING), Set.of(WALK)),
      of(CAR_RENTAL, true, Set.of(HAVE_RENTED, RENTING_FLOATING), Set.of(WALK, CAR)),
      of(StreetMode.CAR, false, NULL_RENTAL_STATES, Set.of(CAR)),
      of(BIKE, false, NULL_RENTAL_STATES, Set.of(BICYCLE)),
      of(StreetMode.WALK, false, NULL_RENTAL_STATES, Set.of(TraverseMode.WALK)),
      of(BIKE_TO_PARK, false, NULL_RENTAL_STATES, Set.of(BICYCLE)),
      of(CAR_TO_PARK, false, NULL_RENTAL_STATES, Set.of(CAR)),
      of(FLEXIBLE, false, NULL_RENTAL_STATES, Set.of(WALK)),
      of(CAR_PICKUP, false, NULL_RENTAL_STATES, Set.of(CAR, WALK)),
      of(CAR_PICKUP, true, NULL_RENTAL_STATES, Set.of(CAR, WALK)),
      of(CAR_HAILING, false, NULL_RENTAL_STATES, Set.of(CAR, WALK)),
      of(CAR_HAILING, true, NULL_RENTAL_STATES, Set.of(CAR, WALK))
    );
  }

  @ParameterizedTest(
    name = "street mode {0}, arriveBy={1} should lead to initial states with rentalStates={2}, currentModes={3}"
  )
  @MethodSource("testCases")
  void initialStates(
    StreetMode streetMode,
    boolean arriveBy,
    Set<VehicleRentalState> expectedRentalStates,
    Set<TraverseMode> expectedStartModes
  ) {
    var req = req(streetMode, arriveBy);

    var states = State.getInitialStates(Set.of(v1), req);

    var vehicleRentalStates = states
      .stream()
      .map(State::getVehicleRentalState)
      .collect(Collectors.toSet());
    assertEquals(expectedRentalStates, vehicleRentalStates);

    var modes = states.stream().map(State::currentMode).collect(Collectors.toSet());
    assertEquals(expectedStartModes, modes);
  }

  private static StreetSearchRequest req(StreetMode streetMode, boolean arriveBy) {
    return StreetSearchRequest.of().withMode(streetMode).withArriveBy(arriveBy).build();
  }

  @Test
  void containsDriving() {
    var state = ofDriving().streetEdge().streetEdge().streetEdge().build();
    assertTrue(state.containsModeCar());
  }

  @Test
  void walking() {
    var state = ofWalking().streetEdge().streetEdge().streetEdge().build();
    assertFalse(state.containsModeCar());
  }

  @Test
  void walkingOnly() {
    // Walk only
    assertTrue(ofWalking().streetEdge().build().containsOnlyWalkMode(), "One edge");
    assertTrue(
      ofWalking().streetEdge().streetEdge().build().containsOnlyWalkMode(),
      "Several edges"
    );

    // Car only
    assertFalse(ofDriving().streetEdge().build().containsOnlyWalkMode(), "One edge");
    assertFalse(
      ofDriving().streetEdge().streetEdge().build().containsOnlyWalkMode(),
      "Several edges"
    );

    assertFalse(
      ofCarRental().streetEdge().pickUpCarFromStation().build().containsOnlyWalkMode(),
      "Walk + CAR"
    );
  }
}
