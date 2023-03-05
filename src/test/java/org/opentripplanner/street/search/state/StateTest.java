package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.CAR;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.SCOOTER_RENTAL;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.search.TraverseMode.BICYCLE;
import static org.opentripplanner.street.search.TraverseMode.WALK;
import static org.opentripplanner.street.search.state.VehicleRentalState.BEFORE_RENTING;
import static org.opentripplanner.street.search.state.VehicleRentalState.HAVE_RENTED;
import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.test.support.VariableSource;

class StateTest {

  Vertex v1 = intersectionVertex(1, 1);

  static Set<VehicleRentalState> NULL_MODES;

  static {
    NULL_MODES = new HashSet<>();
    NULL_MODES.add(null);
  }

  static Stream<Arguments> testCases = Stream.of(
    of(SCOOTER_RENTAL, false, Set.of(BEFORE_RENTING), Set.of(WALK)),
    of(SCOOTER_RENTAL, true, Set.of(HAVE_RENTED, RENTING_FLOATING), Set.of(WALK, BICYCLE)),
    of(BIKE_RENTAL, false, Set.of(BEFORE_RENTING), Set.of(WALK)),
    of(BIKE_RENTAL, true, Set.of(HAVE_RENTED, RENTING_FLOATING), Set.of(WALK, BICYCLE)),
    of(CAR_RENTAL, false, Set.of(BEFORE_RENTING), Set.of(WALK)),
    of(CAR_RENTAL, true, Set.of(HAVE_RENTED, RENTING_FLOATING), Set.of(WALK, BICYCLE)),
    of(CAR, false, NULL_MODES, Set.of(TraverseMode.CAR)),
    of(BIKE, false, NULL_MODES, Set.of(BICYCLE))
  );

  @ParameterizedTest(
    name = "street mode {0}, arriveBy={1} should lead to initial states with rentalStates={2}, currentModes={3}"
  )
  @VariableSource("testCases")
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

    var modes = states.stream().map(State::getNonTransitMode).collect(Collectors.toSet());
    assertEquals(expectedStartModes, modes);
  }

  private static StreetSearchRequest req(StreetMode streetMode, boolean arriveBy) {
    return StreetSearchRequest.of().withMode(streetMode).withArriveBy(arriveBy).build();
  }
}
