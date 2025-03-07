package org.opentripplanner.updater.trip.gtfs.moduletests.cancellation;

import static com.google.common.truth.Truth.assertThat;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

public class CanceledTripTest implements RealtimeTestConstants {

  @Test
  void listCanceledTrips() {
    var env = RealtimeTestEnvironment.of()
      .addTrip(
        TripInput.of(TRIP_1_ID)
          .addStop(STOP_A1, "0:00:10", "0:00:11")
          .addStop(STOP_B1, "0:00:20", "0:00:21")
          .build()
      )
      .build();

    assertThat(env.getTransitService().listCanceledTrips()).isEmpty();

    var update = new TripUpdateBuilder(TRIP_1_ID, SERVICE_DATE, CANCELED, TIME_ZONE).build();
    assertSuccess(env.applyTripUpdate(update));

    var canceled = env.getTransitService().listCanceledTrips();
    assertThat(canceled).hasSize(1);
    var trip = canceled.getFirst();
    assertEquals(id(TRIP_1_ID), trip.getTrip().getId());
    assertEquals(SERVICE_DATE, trip.getServiceDate());
  }
}
