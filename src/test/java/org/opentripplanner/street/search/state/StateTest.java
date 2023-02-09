package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.search.state.VehicleRentalState.BEFORE_RENTING;
import static org.opentripplanner.street.search.state.VehicleRentalState.HAVE_RENTED;
import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.test.support.VariableSource;

class StateTest {

  Vertex v1 = intersectionVertex(1, 1);

  static Stream<Arguments> testCases = Stream.of(
    Arguments.of(false, 1, Set.of(BEFORE_RENTING)),
    Arguments.of(true, 2, Set.of(HAVE_RENTED, RENTING_FLOATING))
  );

  @ParameterizedTest(
    name = "arriveBy={0} should lead to {1} initial state(s) with rental state(s) {2}"
  )
  @VariableSource("testCases")
  void initialRentalStates(
    boolean arriveBy,
    int expectedStateCount,
    Set<VehicleRentalState> expectedRentalStates
  ) {
    var req = req(arriveBy);

    var state = State.getInitialStates(Set.of(v1), req);
    assertEquals(expectedStateCount, state.size());

    var modes = state.stream().map(State::getVehicleRentalState).collect(Collectors.toSet());
    assertEquals(expectedRentalStates, modes);
  }

  private static StreetSearchRequest req(boolean arriveBy) {
    return StreetSearchRequest
      .of()
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withArriveBy(arriveBy)
      .build();
  }
}
