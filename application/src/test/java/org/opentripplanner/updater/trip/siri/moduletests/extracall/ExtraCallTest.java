package org.opentripplanner.updater.trip.siri.moduletests.extracall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;

class ExtraCallTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder ENV_BUILDER = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A1 = ENV_BUILDER.stopAtStation(STOP_A1_ID, "A");
  private final RegularStop STOP_B1 = ENV_BUILDER.stopAtStation(STOP_B1_ID, "B");
  private final RegularStop STOP_C1 = ENV_BUILDER.stopAtStation(STOP_C1_ID, "C");
  private final RegularStop STOP_D1 = ENV_BUILDER.stopAtStation(STOP_D1_ID, "D");

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A1, "0:00:10", "0:00:11")
    .addStop(STOP_B1, "0:00:20", "0:00:21")
    .build();

  @Test
  void testExtraCall() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    var updates = updateWithExtraCall(env);

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A1 [R] 0:00:15 0:00:15 | D1 0:00:20 0:00:25 | B1 0:00:33 0:00:33",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }

  @Test
  void testExtraCallMultipleTimes() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    var updates = updateWithExtraCall(env);
    env.applyEstimatedTimetable(updates);
    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A1 [R] 0:00:15 0:00:15 | D1 0:00:20 0:00:25 | B1 0:00:33 0:00:33",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }

  @Test
  void testExtraCallAndCancellation() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    var updates = updateWithExtraCall(env);
    env.applyEstimatedTimetable(updates);
    var result = env.applyEstimatedTimetable(updates);

    var cancellation = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withCancellation(true)
      .buildEstimatedTimetableDeliveries();

    var cancellationResult = env.applyEstimatedTimetable(cancellation);

    assertEquals(1, cancellationResult.successful());

    assertEquals(1, result.successful());
    assertEquals(
      "CANCELED | A1 0:00:10 0:00:11 | B1 0:00:20 0:00:21",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }

  @Test
  void testExtraUnknownStop() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:11", "00:00:15")
          // Unexpected extra stop without isExtraCall flag
          .call(STOP_D1)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(STOP_B1)
          .arriveAimedExpected("00:00:30", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.TOO_MANY_STOPS, result);
  }

  @Test
  void testExtraCallSameNumberOfStops() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:11", "00:00:15")
          // Unexpected ExtraCall flag on a scheduled stop
          .call(STOP_B1)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:30", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE, result);
  }

  @Test
  void testExtraCallAndIllegalChangeOfOtherStops() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_D1)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          // this scheduled stop should not be changed
          .call(STOP_C1)
          .arriveAimedExpected("00:00:30", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.STOP_MISMATCH, result);
  }

  private List<EstimatedTimetableDeliveryStructure> updateWithExtraCall(
    RealtimeTestEnvironment env
  ) {
    return new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withLineRef(TRIP_1_INPUT.routeId())
      .withRecordedCalls(builder -> builder.call(STOP_A1).departAimedActual("00:00:11", "00:00:15"))
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_D1)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:18", "00:00:20")
          .departAimedExpected("00:00:19", "00:00:25")
          .call(STOP_B1)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();
  }
}
