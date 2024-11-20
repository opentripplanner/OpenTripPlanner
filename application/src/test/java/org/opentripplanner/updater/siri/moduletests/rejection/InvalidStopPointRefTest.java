package org.opentripplanner.updater.siri.moduletests.rejection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.updater.siri.SiriEtBuilder;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;

public class InvalidStopPointRefTest implements RealtimeTestConstants {

  @ParameterizedTest
  @ValueSource(strings = { "", " ", "\n", "null", "   " })
  void rejectEmptyStopPointRef(String invalidRef) {
    var env = RealtimeTestEnvironment.siri().build();

    // journey contains empty stop point ref elements
    // happens in the South Tyrolian feed: https://github.com/noi-techpark/odh-mentor-otp/issues/213
    var invalidJourney = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("invalid-journey")
      .withOperatorRef("unknown-operator")
      .withLineRef("unknown-line")
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
