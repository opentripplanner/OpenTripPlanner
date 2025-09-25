package org.opentripplanner.updater.trip.gtfs.moduletests.addition;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.NEW;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_ALREADY_EXISTS;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

class TripAlreadyExistsTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder envBuilder = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A = envBuilder.stop(STOP_A_ID);
  private final RegularStop STOP_B = envBuilder.stop(STOP_B_ID);

  private final RealtimeTestEnvironment env = envBuilder
    .addTrip(TripInput.of(TRIP_1_ID).addStop(STOP_A, "12:00").addStop(STOP_B, "13:00").build())
    .build();

  @Test
  void scheduledTripAlreadyExists() {
    var tripUpdate = new TripUpdateBuilder(TRIP_1_ID, SERVICE_DATE, NEW, TIME_ZONE)
      .addStopTime(STOP_A_ID, "10:30")
      .addStopTime(STOP_B_ID, "10:40")
      .build();

    assertFailure(TRIP_ALREADY_EXISTS, env.applyTripUpdate(tripUpdate));
  }
}
