package org.opentripplanner.updater.siri.moduletests.rejection;

import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.siri.SiriEtBuilder;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;

class NotMonitoredTest implements RealtimeTestConstants {

  private static final TripInput TRIP_1_INPUT = TripInput
    .of(TRIP_1_ID)
    .withRoute(ROUTE_1.copy().withOperator(OPERATOR1).build())
    .addStop(STOP_A1, "0:00:10", "0:00:11")
    .addStop(STOP_B1, "0:00:20", "0:00:21")
    .build();

  @Test
  void testNotMonitored() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withMonitored(false)
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.NOT_MONITORED, result);
  }
}
