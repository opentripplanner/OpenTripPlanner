package org.opentripplanner.ext.ridehailing;

import static graphql.Assert.assertTrue;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.test.support.VariableSource;

class RideHailingDepartureTimeShifterTest {

  private static final Instant NOW = OffsetDateTime.parse("2023-03-23T17:13:46+01:00").toInstant();

  RideHailingService service = new TestRideHailingService(
    TestRideHailingService.DEFAULT_ARRIVAL_TIMES,
    List.of()
  );

  static Stream<Arguments> testCases = Stream.of(
    // leave now, so shift by 10 minutes
    Arguments.of(NOW, TestRideHailingService.DEFAULT_ARRIVAL_DURATION),
    // only shift by 9 minutes because we are wanting to leave in 1 minute
    Arguments.of(NOW.plus(ofMinutes(1)), ofMinutes(9)),
    // only shift by 7 minutes because we are wanting to leave in 3 minutes
    Arguments.of(NOW.plus(ofMinutes(3)), ofMinutes(7)),
    // no shifting because it's in the future
    Arguments.of(NOW.plus(ofMinutes(15)), ZERO),
    Arguments.of(NOW.plus(ofMinutes(30)), ZERO),
    Arguments.of(NOW.plus(ofMinutes(40)), ZERO)
  );

  @ParameterizedTest
  @VariableSource("testCases")
  void arrivalDelay(Instant searchTime, Duration expectedArrival) {
    var req = new RouteRequest();
    req.setTo(new GenericLocation(0d, 0d));
    req.setFrom(new GenericLocation(0d, 0d));
    req.setDateTime(searchTime);
    req.journey().setModes(RequestModes.of().withAccessMode(StreetMode.CAR_HAILING).build());

    var result = RideHailingDepartureTimeShifter.arrivalDelay(req, List.of(service), NOW);

    assertTrue(result.isSuccess());
    var actualArrival = result.successValue();

    // start time should be shifted by 10 minutes
    assertEquals(expectedArrival, actualArrival);
  }
}
