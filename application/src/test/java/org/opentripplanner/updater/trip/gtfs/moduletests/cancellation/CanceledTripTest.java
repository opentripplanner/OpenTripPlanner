package org.opentripplanner.updater.trip.gtfs.moduletests.cancellation;

import static com.google.common.truth.Truth.assertThat;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

public class CanceledTripTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);

  @Test
  void listCanceledTrips() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of(TRIP_1_ID)
        .addStop(STOP_A, "0:00:10", "0:00:11")
        .addStop(STOP_B, "0:00:20", "0:00:21")
    ).build();
    var rt = GtfsRtTestHelper.of(env);

    assertThat(env.transitService().listCanceledTrips()).isEmpty();

    var update = rt.tripUpdate(TRIP_1_ID, CANCELED).build();
    assertSuccess(rt.applyTripUpdate(update));

    var canceled = env.transitService().listCanceledTrips();
    assertThat(canceled).hasSize(1);
    var trip = canceled.getFirst();
    assertEquals(id(TRIP_1_ID), trip.getTrip().getId());
    assertEquals(env.defaultServiceDate(), trip.getServiceDate());
  }
}
