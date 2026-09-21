package org.opentripplanner.updater.trip.siri.moduletests.replacement;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.network.ReplacedByRelation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;

class PartialReplacementTest implements RealtimeTestConstants {

  private final String CIRCULAR_TRIP_ID = "CircularTrip";

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);
  private final RegularStop STOP_D = ENV_BUILDER.stop(STOP_D_ID);
  private final RegularStop STOP_E = ENV_BUILDER.stop(STOP_E_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStop(STOP_A, "0:01")
    .addStop(STOP_B, "0:02")
    .addStop(STOP_C, "0:03")
    .addStop(STOP_D, "0:04")
    .addStop(STOP_E, "0:05");

  private final TripInput TRIP_2_INPUT = TripInput.of(TRIP_2_ID)
    .withWithTripOnServiceDate(TRIP_2_ID)
    .addStop(STOP_C, "0:03")
    .addStop(STOP_D, "0:04")
    .addStop(STOP_E, "0:05");

  private final TripInput CIRCULAR_TRIP_INPUT = TripInput.of(CIRCULAR_TRIP_ID)
    .withWithTripOnServiceDate(CIRCULAR_TRIP_ID)
    .addStop(STOP_A, "0:01")
    .addStop(STOP_B, "0:02")
    .addStop(STOP_A, "0:03")
    .addStop(STOP_B, "0:04");

  @Test
  void testPartialReplacement() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).addTrip(TRIP_2_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:01", "00:01")
          .call(STOP_B)
          .arriveAimedExpected("00:02", "00:02")
          .departAimedExpected("00:02", "00:02")
          .call(STOP_C)
          .arriveAimedExpected("00:03", "00:03")
          .departAimedExpected("00:03", "00:03")
          .call(STOP_D)
          .withCancellation(true)
          .call(STOP_E)
          .withCancellation(true)
      )
      .withPartialReplacement(STOP_C, STOP_E, TRIP_2_ID, env.defaultServiceDate())
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "P U | A 0:01 0:01 | B 0:02 0:02 | C 0:03 0:03 | D [C] 0:04 0:04 | E [C] 0:05 0:05",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    var tt = env.tripData(TRIP_1_ID).tripTimes();
    var trip2 = env.tripData(TRIP_2_ID).tripOnServiceDate().getId();

    assertThat(tt.getArrivalReplacedByRelations(0)).isEmpty();
    assertThat(tt.getArrivalReplacedByRelations(1)).isEmpty();
    assertThat(tt.getArrivalReplacedByRelations(2)).isEmpty();
    assertReplacedBy(tt.getArrivalReplacedByRelations(3), trip2);
    assertReplacedBy(tt.getArrivalReplacedByRelations(4), trip2);

    assertThat(tt.getDepartureReplacedByRelations(0)).isEmpty();
    assertThat(tt.getDepartureReplacedByRelations(1)).isEmpty();
    assertReplacedBy(tt.getDepartureReplacedByRelations(2), trip2);
    assertReplacedBy(tt.getDepartureReplacedByRelations(3), trip2);
    assertThat(tt.getDepartureReplacedByRelations(4)).isEmpty();
  }

  /**
   * If a partial replacements references an unknown dated service journey it is dropped.
   */
  @Test
  void testUnknownReplacement() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).addTrip(TRIP_2_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:01", "00:01")
          .call(STOP_B)
          .arriveAimedExpected("00:02", "00:02")
          .departAimedExpected("00:02", "00:02")
          .call(STOP_C)
          .arriveAimedExpected("00:03", "00:03")
          .departAimedExpected("00:03", "00:03")
          .call(STOP_D)
          .withCancellation(true)
          .call(STOP_E)
          .withCancellation(true)
      )
      .withPartialReplacement(STOP_C, STOP_E, "UnknownId", env.defaultServiceDate())
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "P U | A 0:01 0:01 | B 0:02 0:02 | C 0:03 0:03 | D [C] 0:04 0:04 | E [C] 0:05 0:05",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    var tt = env.tripData(TRIP_1_ID).tripTimes();

    assertThat(tt.getArrivalReplacedByRelations(0)).isEmpty();
    assertThat(tt.getArrivalReplacedByRelations(1)).isEmpty();
    assertThat(tt.getArrivalReplacedByRelations(2)).isEmpty();
    assertThat(tt.getArrivalReplacedByRelations(3)).isEmpty();
    assertThat(tt.getArrivalReplacedByRelations(4)).isEmpty();
  }

  /**
   * If a stop is passed more than once, the first stop is chosen by default
   */
  @Test
  void testCircularRoutePickFirst() {
    var env = ENV_BUILDER.addTrip(CIRCULAR_TRIP_INPUT).addTrip(TRIP_2_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(CIRCULAR_TRIP_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:01", "00:11")
          .call(STOP_B)
          .arriveAimedExpected("00:02", "00:12")
          .departAimedExpected("00:02", "00:12")
          .call(STOP_A)
          .arriveAimedExpected("00:03", "00:13")
          .departAimedExpected("00:03", "00:13")
          .call(STOP_B)
          .arriveAimedExpected("00:04", "00:14")
      )
      .withPartialReplacement(STOP_A, STOP_B, TRIP_2_ID, env.defaultServiceDate())
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "U | A 0:11 0:11 | B 0:12 0:12 | A 0:13 0:13 | B 0:14 0:14",
      env.tripData(CIRCULAR_TRIP_ID).showTimetable()
    );
    var tt = env.tripData(CIRCULAR_TRIP_ID).tripTimes();
    var trip2 = env.tripData(TRIP_2_ID).tripOnServiceDate().getId();

    assertThat(tt.getArrivalReplacedByRelations(0)).isEmpty();
    assertReplacedBy(tt.getArrivalReplacedByRelations(1), trip2);
    assertThat(tt.getArrivalReplacedByRelations(2)).isEmpty();
    assertThat(tt.getArrivalReplacedByRelations(3)).isEmpty();

    assertReplacedBy(tt.getDepartureReplacedByRelations(0), trip2);
    assertThat(tt.getDepartureReplacedByRelations(1)).isEmpty();
    assertThat(tt.getDepartureReplacedByRelations(2)).isEmpty();
    assertThat(tt.getDepartureReplacedByRelations(3)).isEmpty();
  }

  @Test
  void testCircularRouteDisambiguateByTime() {
    var env = ENV_BUILDER.addTrip(CIRCULAR_TRIP_INPUT).addTrip(TRIP_2_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(CIRCULAR_TRIP_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:01", "00:11")
          .call(STOP_B)
          .arriveAimedExpected("00:02", "00:12")
          .departAimedExpected("00:02", "00:12")
          .call(STOP_A)
          .arriveAimedExpected("00:03", "00:13")
          .departAimedExpected("00:03", "00:13")
          .call(STOP_B)
          .arriveAimedExpected("00:04", "00:14")
      )
      .withPartialReplacement(STOP_A, "00:03", STOP_B, "00:04", TRIP_2_ID, env.defaultServiceDate())
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "U | A 0:11 0:11 | B 0:12 0:12 | A 0:13 0:13 | B 0:14 0:14",
      env.tripData(CIRCULAR_TRIP_ID).showTimetable()
    );
    var tt = env.tripData(CIRCULAR_TRIP_ID).tripTimes();
    var trip2 = env.tripData(TRIP_2_ID).tripOnServiceDate().getId();

    assertThat(tt.getArrivalReplacedByRelations(0)).isEmpty();
    assertThat(tt.getArrivalReplacedByRelations(1)).isEmpty();
    assertThat(tt.getArrivalReplacedByRelations(2)).isEmpty();
    assertReplacedBy(tt.getArrivalReplacedByRelations(3), trip2);

    assertThat(tt.getDepartureReplacedByRelations(0)).isEmpty();
    assertThat(tt.getDepartureReplacedByRelations(1)).isEmpty();
    assertReplacedBy(tt.getDepartureReplacedByRelations(2), trip2);
    assertThat(tt.getDepartureReplacedByRelations(3)).isEmpty();
  }

  private void assertReplacedBy(List<ReplacedByRelation> replacedBys, FeedScopedId expectedId) {
    assertEquals(1, replacedBys.size());
    assertEquals(expectedId, replacedBys.getFirst().getTripOnServiceDate().getId());
  }
}
