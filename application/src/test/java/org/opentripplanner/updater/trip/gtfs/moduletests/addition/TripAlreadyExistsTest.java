package org.opentripplanner.updater.trip.gtfs.moduletests.addition;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.NEW;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_ALREADY_EXISTS;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.SiteTestBuilder;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

class TripAlreadyExistsTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(
    SiteTestBuilder.of().withStops(STOP_A_ID, STOP_B_ID).build()
  );

  private final TransitTestEnvironment env = envBuilder
    .addTrip(
      TripInput.of(TRIP_1_ID).addStop(STOP_A_ID, "12:00").addStop(STOP_B_ID, "13:00").build()
    )
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
