package org.opentripplanner.updater.trip.gtfs.moduletests.addition;

import static com.google.common.truth.Truth.assertThat;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.NEW;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertNoFailure;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

class AddedThenRemovedTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop stopA = envBuilder.stop(STOP_A_ID);
  private final RegularStop stopB = envBuilder.stop(STOP_B_ID);
  private final RegularStop stopC = envBuilder.stop(STOP_C_ID);

  private final TransitTestEnvironment env = envBuilder
    .addTrip(
      TripInput.of(TRIP_1_ID)
        .addStop(stopA, "12:00")
        .addStop(stopB, "12:10")
        .addStop(stopC, "12:20")
    )
    .build();
  private final GtfsRtTestHelper rt = GtfsRtTestHelper.of(env);

  @Test
  void addedThenRemoved() {
    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Pattern1[SCHEDULED]");

    var tripUpdate = rt
      .tripUpdate(ADDED_TRIP_ID, NEW)
      .addStopTime(STOP_A_ID, "10:30")
      .addStopTime(STOP_B_ID, "10:40")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    assertThat(env.raptorData().summarizePatterns()).containsExactly(
      "F:Pattern1[SCHEDULED]",
      "F:AddedTrip::rt#1[ADDED]"
    );

    // the GTFS updater is configured to clear timetables, so an empty list should remove the
    // previously added one
    assertNoFailure(rt.applyTripUpdates(List.of()));

    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Pattern1[SCHEDULED]");
  }
}
