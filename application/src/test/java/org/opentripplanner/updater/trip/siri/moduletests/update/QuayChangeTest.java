package org.opentripplanner.updater.trip.siri.moduletests.update;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class QuayChangeTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder ENV_BUILDER = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A1 = ENV_BUILDER.stop(STOP_A1_ID);
  private final RegularStop STOP_B1 = ENV_BUILDER.stopAtStation(STOP_B1_ID, STATION_B_ID);
  private final RegularStop STOP_B2 = ENV_BUILDER.stopAtStation(STOP_B2_ID, STATION_B_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A1, "0:00:10", "0:00:11")
    .addStop(STOP_B1, "0:00:20", "0:00:21")
    .build();

  /**
   * Change quay on a trip
   */
  @Test
  void testChangeQuay() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A1).departAimedActual("00:00:11", "00:00:15"))
      .withEstimatedCalls(builder ->
        builder.call(STOP_B2).arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A1 [R] 0:00:15 0:00:15 | B2 0:00:33 0:00:33",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }
}
