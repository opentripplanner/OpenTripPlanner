package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

class AccessEgressFilterTest {

  private static final int LIMIT = 45;

  private static final DefaultAccessEgress CAR_RENTAL_TOO_SHORT = ofCarRental(LIMIT - 1);
  private static final DefaultAccessEgress CAR_RENTAL_OK = ofCarRental(LIMIT);

  // Walking should not be filtered even if it is below the limit
  private static final DefaultAccessEgress WALK = ofWalking(LIMIT - 10);

  private static List<Arguments> filterMinDurationTestCase() {
    return List.of(
      Arguments.of(List.of(), List.of()),
      Arguments.of(List.of(WALK), List.of(WALK)),
      Arguments.of(List.of(CAR_RENTAL_OK), List.of(CAR_RENTAL_OK)),
      Arguments.of(List.of(), List.of(CAR_RENTAL_TOO_SHORT)),
      Arguments.of(List.of(WALK, CAR_RENTAL_OK), List.of(WALK, CAR_RENTAL_TOO_SHORT, CAR_RENTAL_OK))
    );
  }

  @ParameterizedTest
  @MethodSource("filterMinDurationTestCase")
  void filterMinDurationAccessTest(
    List<DefaultAccessEgress> expected,
    List<DefaultAccessEgress> input
  ) {
    var requestLimitAccess = createRequestWithMinDurationLimitForCar();
    requestLimitAccess
      .journey()
      .setModes(RequestModes.of().withAccessMode(StreetMode.CAR_RENTAL).build());

    var filterAccess = new AccessEgressFilter(requestLimitAccess);

    assertEquals(expected, filterAccess.filterAccess(input));
    assertEquals(input, filterAccess.filterEgress(input));
  }

  @ParameterizedTest
  @MethodSource("filterMinDurationTestCase")
  void filterMinDurationEgressTest(
    List<DefaultAccessEgress> expected,
    List<DefaultAccessEgress> input
  ) {
    var requestLimitEgress = createRequestWithMinDurationLimitForCar();
    requestLimitEgress
      .journey()
      .setModes(RequestModes.of().withEgressMode(StreetMode.CAR_RENTAL).build());

    var filterEgress = new AccessEgressFilter(requestLimitEgress);

    assertEquals(input, filterEgress.filterAccess(input));
    assertEquals(expected, filterEgress.filterEgress(input));
  }

  private RouteRequest createRequestWithMinDurationLimitForCar() {
    RouteRequest requestLimitAccess = new RouteRequest();
    requestLimitAccess.withPreferences(p ->
      p.withStreet(s ->
        s.withMinAccessEgressDuration(b -> b.with(StreetMode.CAR_RENTAL, Duration.ofSeconds(LIMIT)))
      )
    );
    return requestLimitAccess;
  }

  @Test
  void filterEgress() {}

  private static DefaultAccessEgress ofCarRental(int duration) {
    return ofAccessEgress(
      duration,
      TestStateBuilder.ofCarRental().streetEdge().pickUpCar().build()
    );
  }

  private static DefaultAccessEgress ofWalking(int duration) {
    return ofAccessEgress(duration, TestStateBuilder.ofWalking().streetEdge().build());
  }

  private static DefaultAccessEgress ofAccessEgress(int duration, State state) {
    return new DefaultAccessEgress(1, state) {
      @Override
      public int durationInSeconds() {
        return duration;
      }
    };
  }
}
