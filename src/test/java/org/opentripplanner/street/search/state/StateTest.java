package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.search.state.VehicleRentalState.BEFORE_RENTING;
import static org.opentripplanner.street.search.state.VehicleRentalState.HAVE_RENTED;
import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class StateTest {

  Vertex v1 = intersectionVertex(1, 1);

  @Test
  void forwardsInitialRentalStates() {
    var req = req(false);

    var state = State.getInitialStates(Set.of(v1), req);
    assertEquals(1, state.size());

    var modes = state.stream().map(State::getVehicleRentalState).collect(Collectors.toSet());
    assertEquals(Set.of(BEFORE_RENTING), modes);
  }

  @Test
  void backwardsInitialRentalStates() {
    var req = req(true);

    var state = State.getInitialStates(Set.of(v1), req);
    assertEquals(2, state.size());

    var modes = state.stream().map(State::getVehicleRentalState).collect(Collectors.toSet());
    assertEquals(Set.of(RENTING_FLOATING, HAVE_RENTED), modes);
  }

  private static StreetSearchRequest req(boolean arriveBy) {
    return StreetSearchRequest
      .of()
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withArriveBy(arriveBy)
      .build();
  }
}
