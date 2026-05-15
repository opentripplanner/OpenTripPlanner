package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsService;
import org.opentripplanner.street.internal.notes.StreetNotesService;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.service.NoopSiteResolver;

class LegsToItineraryMapperTest {

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
    var mapper = new StreetPathToLegsMapper(
      new NoopSiteResolver(),
      ZoneIds.UTC,
      new StreetNotesService(),
      new DefaultStreetDetailsService(new DefaultStreetDetailsRepository()),
      1
    );
    var path = new StreetPath(state);
    var legs = mapper.map(path, RouteRequest.defaultValue());
    var itin = LegsToItineraryMapper.map(legs, false, null).get();
    assertFalse(itin.isSearchWindowAware());
  }
}
