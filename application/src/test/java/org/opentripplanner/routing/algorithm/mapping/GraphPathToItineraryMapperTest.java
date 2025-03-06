package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

class GraphPathToItineraryMapperTest {

  private static Stream<Arguments> cases() {
    return Stream.of(
      TestStateBuilder.ofWalking(),
      TestStateBuilder.ofCycling(),
      TestStateBuilder.ofDriving(),
      TestStateBuilder.ofScooterRental().pickUpFreeFloatingScooter(),
      TestStateBuilder.ofBikeAndRide(),
      TestStateBuilder.parkAndRide()
    ).map(b -> {
      var state = b.streetEdge().streetEdge().build();
      return Arguments.argumentSet(state.currentMode().toString(), state);
    });
  }

  @ParameterizedTest
  @MethodSource("cases")
  void isSearchWindowAware(State state) {
    var mapper = new GraphPathToItineraryMapper(ZoneIds.UTC, new StreetNotesService(), 1);
    var itin = mapper.generateItinerary(new GraphPath<>(state));
    assertFalse(itin.isSearchWindowAware());
  }
}
