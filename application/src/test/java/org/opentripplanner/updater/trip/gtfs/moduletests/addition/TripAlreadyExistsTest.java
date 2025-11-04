package org.opentripplanner.updater.trip.gtfs.moduletests.addition;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.NEW;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_ALREADY_EXISTS;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

class TripAlreadyExistsTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop STOP_A = envBuilder.stop(STOP_A_ID);
  private final RegularStop STOP_B = envBuilder.stop(STOP_B_ID);

  private final TransitTestEnvironment env = envBuilder
    .addTrip(TripInput.of(TRIP_1_ID).addStop(STOP_A, "12:00").addStop(STOP_B, "13:00"))
    .build();
  private final GtfsRtTestHelper rt = GtfsRtTestHelper.of(env);

  @Test
  void scheduledTripAlreadyExists() {
    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, NEW)
      .addStopTime(STOP_A_ID, "10:30")
      .addStopTime(STOP_B_ID, "10:40")
      .build();

    assertFailure(TRIP_ALREADY_EXISTS, rt.applyTripUpdate(tripUpdate));
  }
}
