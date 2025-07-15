package org.opentripplanner.ext.ridehailing;

import static graphql.Assert.assertTrue;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.ridehailing.RideHailingAccessShifter.arrivalDelay;
import static org.opentripplanner.ext.ridehailing.TestRideHailingService.DEFAULT_ARRIVAL_DURATION;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

class RideHailingAccessShifterTest {

  private static final Instant TIME = OffsetDateTime.parse("2023-03-23T17:00:00+01:00").toInstant();
  private static final GenericLocation FROM = GenericLocation.fromCoordinate(0d, 0d);
  private static final GenericLocation TO = GenericLocation.fromCoordinate(1d, 1d);
  private static final State DRIVING_STATE = TestStateBuilder.ofDriving()
    .streetEdge()
    .streetEdge()
    .build();

  RideHailingService service = new TestRideHailingService(
    TestRideHailingService.DEFAULT_ARRIVAL_TIMES,
    List.of()
  );

  static Stream<Arguments> testCases() {
    return Stream.of(
      // leave now, so shift by 10 minutes
      Arguments.of(TIME, DEFAULT_ARRIVAL_DURATION),
      // only shift by 9 minutes because we are wanting to leave in 1 minute
      Arguments.of(TIME.plus(ofMinutes(1)), ofMinutes(9)),
      // only shift by 7 minutes because we are wanting to leave in 3 minutes
      Arguments.of(TIME.plus(ofMinutes(3)), ofMinutes(7)),
      // no shifting because it's far in the future
      Arguments.of(TIME.plus(ofMinutes(15)), ZERO),
      Arguments.of(TIME.plus(ofMinutes(30)), ZERO),
      Arguments.of(TIME.plus(ofMinutes(40)), ZERO)
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testArrivalDelay(Instant searchTime, Duration expectedArrival) {
    var req = RouteRequest.of()
      .withTo(FROM)
      .withFrom(TO)
      .withDateTime(searchTime)
      .withJourney(jb ->
        jb.setModes(RequestModes.of().withAccessMode(StreetMode.CAR_HAILING).build())
      )
      .buildRequest();

    var result = arrivalDelay(req, List.of(service), TIME);

    assertTrue(result.isSuccess());
    var actualArrival = result.successValue();

    // start time should be shifted by 10 minutes
    assertEquals(expectedArrival, actualArrival);
  }

  static Stream<Arguments> accessShiftCases() {
    return Stream.of(
      // leave now, so shift by 10 minutes
      Arguments.of(TIME, TIME.plus(DEFAULT_ARRIVAL_DURATION)),
      Arguments.of(TIME.plus(Duration.ofHours(4)), TIME)
    );
  }

  @ParameterizedTest
  @MethodSource("accessShiftCases")
  void shiftAccesses(Instant startTime, Instant expectedStartTime) {
    var drivingState = TestStateBuilder.ofDriving().streetEdge().streetEdge().build();
    var access = new DefaultAccessEgress(0, drivingState);

    RouteRequest req = routeRequest(startTime);

    var shifted = RideHailingAccessShifter.shiftAccesses(
      true,
      List.of(access),
      List.of(service),
      req,
      TIME
    );

    var shiftedAccess = shifted.get(0);

    var shiftedStart = shiftedAccess.earliestDepartureTime(
      TIME.atZone(ZoneIds.BERLIN).toLocalTime().toSecondOfDay()
    );

    var earliestStartTime = LocalTime.ofSecondOfDay(shiftedStart);

    assertEquals(expectedStartTime.atZone(ZoneIds.BERLIN).toLocalTime(), earliestStartTime);
  }

  @Test
  void failingService() {
    var access = new DefaultAccessEgress(0, DRIVING_STATE);
    RouteRequest req = routeRequest(TIME);

    var shifted = RideHailingAccessShifter.shiftAccesses(
      true,
      List.of(access),
      List.of(new FailingRideHailingService()),
      req,
      TIME
    );

    assertEquals(List.of(), shifted);
  }

  private static RouteRequest routeRequest(Instant time) {
    return RouteRequest.of()
      .withDateTime(time)
      .withFrom(FROM)
      .withTo(TO)
      .withJourney(jb ->
        jb.setModes(RequestModes.of().withAccessMode(StreetMode.CAR_HAILING).build())
      )
      .buildRequest();
  }
}
