package org.opentripplanner.updater.trip.siri.moduletests.rejection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class InvalidStopPointRefTest {

  private static Stream<Arguments> cases() {
    return Stream.of("", " ", "   ", "\n", "null", "\t", null).flatMap(id ->
      Stream.of(Arguments.of(id, true), Arguments.of(id, false))
    );
  }

  @ParameterizedTest(name = "invalid id of ''{0}'', extraJourney={1}")
  @MethodSource("cases")
  void rejectEmptyStopPointRef(String invalidRef, boolean extraJourney) {
    var env = RealtimeTestEnvironment.of().build();

    // journey contains empty stop point ref elements
    // happens in the South Tyrolian feed: https://github.com/noi-techpark/odh-mentor-otp/issues/213
    var invalidJourney = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("invalid-journey")
      .withOperatorRef("unknown-operator")
      .withLineRef("unknown-line")
      .withIsExtraJourney(extraJourney)
      .withEstimatedCalls(builder ->
        builder
          .call(invalidRef)
          .departAimedExpected("10:58", "10:48")
          .call(invalidRef)
          .arriveAimedExpected("10:08", "10:58")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(invalidJourney);
    assertEquals(0, result.successful());
    assertFailure(UpdateError.UpdateErrorType.EMPTY_STOP_POINT_REF, result);
  }
}
