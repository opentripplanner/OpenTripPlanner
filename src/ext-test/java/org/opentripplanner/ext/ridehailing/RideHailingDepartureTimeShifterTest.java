package org.opentripplanner.ext.ridehailing;

import static graphql.Assert.assertTrue;
import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.test.support.VariableSource;

class RideHailingDepartureTimeShifterTest {

  static Instant instant = OffsetDateTime.parse("2023-03-23T17:13:46+01:00").toInstant();

  RideHailingService service = new TestRideHailingService(
    TestRideHailingService.DEFAULT_ARRIVAL_TIMES,
    List.of()
  );

  static Stream<Arguments> testCases = Stream.of(
    // leave now, so shift by 10 minutes
    Arguments.of(instant, instant.plus(TestRideHailingService.DEFAULT_ARRIVAL_DURATION)),
    Arguments.of(instant.plus(ofMinutes(15)), instant.plus(ofMinutes(15))),
    // no shifting because it's in the future
    Arguments.of(instant.plus(ofMinutes(30)), instant.plus(ofMinutes(30))),
    Arguments.of(instant.plus(ofMinutes(40)), instant.plus(ofMinutes(40))),
    // only shift by 9 minutes because we are wanting to leave in one minute
    Arguments.of(instant.plus(ofMinutes(1)), instant.plus(ofMinutes(10)))
  );

  @ParameterizedTest
  @VariableSource("testCases")
  void shift(Instant searchTime, Instant expectedTimeAfterShifting) {
    var req = new RouteRequest();
    req.setTo(new GenericLocation(0d, 0d));
    req.setFrom(new GenericLocation(0d, 0d));
    req.setDateTime(searchTime);
    req.journey().setModes(RequestModes.of().withAccessMode(StreetMode.CAR_HAILING).build());

    var result = RideHailingDepartureTimeShifter.shiftDepartureTime(req, List.of(service), instant);

    assertTrue(result.isSuccess());
    var shifted = result.successValue();

    var time = OffsetDateTime.ofInstant(shifted.dateTime(), ZoneIds.BERLIN);

    var expectedTime = OffsetDateTime.ofInstant(expectedTimeAfterShifting, ZoneIds.BERLIN);

    // start time should be shifted by 10 minutes
    assertEquals(expectedTime.toString(), time.toString());
  }
}
