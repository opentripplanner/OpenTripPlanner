package org.opentripplanner.updater.trip.siri.moduletests.cancellation;

import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;

/**
 * Test that cancelling the same stop on two trips from different routes (but with the same stop
 * pattern) does not cause cross-contamination via TripPatternCache.
 * <p>
 * TripPatternCache caches RT patterns keyed by StopPattern only, setting
 * originalTripPattern from the first trip. When a second trip on a different route produces the
 * same modified StopPattern, the cache returns a pattern with the wrong originalTripPattern.
 * If the updater uses originalTripPattern to identify the original scheduled pattern of a
 * modified trip, it will return the wrong result (symptom: when looking up for TripTimes
 * in the wrong pattern's timetable, it will get null → TRIP_NOT_FOUND_IN_PATTERN).
 * TODO RT_VP this design flaw is documented also in TripPatternCache
 *      and org.opentripplanner.transit.model.network.TripPattern#getOriginalTripPattern()
 */
class CancelledStopCrossPatternTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_D = ENV_BUILDER.stop(STOP_D_ID);

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .withRoute(ENV_BUILDER.route("Route1"))
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStop(STOP_A, "0:01:00", "0:01:01")
    .addStop(STOP_B, "0:01:10", "0:01:11")
    .addStop(STOP_D, "0:01:20", "0:01:21");

  private final TripInput TRIP_2_INPUT = TripInput.of(TRIP_2_ID)
    .withRoute(ENV_BUILDER.route("Route2"))
    .withWithTripOnServiceDate(TRIP_2_ID)
    .addStop(STOP_A, "0:02:00", "0:02:01")
    .addStop(STOP_B, "0:02:10", "0:02:11")
    .addStop(STOP_D, "0:02:20", "0:02:21");

  @Test
  void cancelledStopOnTwoTripsFromDifferentRoutes() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).addTrip(TRIP_2_INPUT).build();
    var siri = SiriTestHelper.of(env);

    // Batch 1: Cancel stop B on TRIP_1 — creates TripPatternCache entry
    var batch1 = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:01:01", "00:01:01")
          .call(STOP_B)
          .withIsCancellation(true)
          .call(STOP_D)
          .arriveAimedExpected("00:01:20", "00:01:20")
      )
      .buildEstimatedTimetableDeliveries();
    assertSuccess(siri.applyEstimatedTimetable(batch1));

    // Batch 2: Cancel stop B on TRIP_2 — reuses cached RT pattern (wrong originalTripPattern)
    var batch2 = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_2_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:02:01", "00:02:01")
          .call(STOP_B)
          .withIsCancellation(true)
          .call(STOP_D)
          .arriveAimedExpected("00:02:20", "00:02:20")
      )
      .buildEstimatedTimetableDeliveries();
    assertSuccess(siri.applyEstimatedTimetable(batch2));

    // Batch 3: Cancel stop B on TRIP_2 again (differential update)
    // if matching with originalTripPattern, this fails with TRIP_NOT_FOUND_IN_PATTERN because
    // findPattern(trip2, serviceDate) returns the contaminated RT pattern
    // whose originalTripPattern belongs to TRIP_1's route.
    var batch3 = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_2_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:02:01", "00:02:01")
          .call(STOP_B)
          .withIsCancellation(true)
          .call(STOP_D)
          .arriveAimedExpected("00:02:20", "00:02:20")
      )
      .buildEstimatedTimetableDeliveries();
    assertSuccess(siri.applyEstimatedTimetable(batch3));
  }
}
