package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graphfinder.NoopSiteResolver;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsService;
import org.opentripplanner.street.internal.notes.StreetNotesService;
import org.opentripplanner.street.search.StreetPath;
import org.opentripplanner.street.search.TraverseMode;
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
    var mapper = new GraphPathToItineraryMapper(
      new NoopSiteResolver(),
      ZoneIds.UTC,
      new StreetNotesService(),
      new DefaultStreetDetailsService(new DefaultStreetDetailsRepository()),
      1
    );
    var itin = mapper.generateItinerary(new StreetPath(state), RouteRequest.defaultValue());
    assertFalse(itin.isSearchWindowAware());
  }

  @Test
  void testPickUpCar() {
    var mapper = mapper();

    var state = TestStateBuilder.ofCarRental().streetEdge().pickUpCarFromStation().build();

    var res = mapper.generateItinerary(new StreetPath(state), RouteRequest.defaultValue());

    var legs = res.legs();
    assertEquals(2, legs.size());
    assertEquals(TraverseMode.WALK, traverseMode(legs.get(0)));
    assertEquals(TraverseMode.CAR, traverseMode(legs.get(1)));
  }

  private TraverseMode traverseMode(Leg leg) {
    return ((StreetLeg) leg).getMode();
  }

  private GraphPathToItineraryMapper mapper() {
    return new GraphPathToItineraryMapper(
      new NoopSiteResolver(),
      ZoneIds.UTC,
      new StreetNotesService(),
      new DefaultStreetDetailsService(new DefaultStreetDetailsRepository()),
      1
    );
  }
}
