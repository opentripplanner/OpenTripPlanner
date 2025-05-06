package org.opentripplanner.updater.trip.gtfs.moduletests.cancellation;

import static com.google.common.truth.Truth.assertThat;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

public class CanceledTripTest {

  private static final RealtimeTestConstants CONSTANTS = new RealtimeTestConstants();
  private static final String TRIP_1_ID = CONSTANTS.TRIP_1_ID;
  private static final RegularStop STOP_A1 = CONSTANTS.STOP_A1;
  private static final RegularStop STOP_B1 = CONSTANTS.STOP_B1;
  private static final LocalDate SERVICE_DATE = CONSTANTS.SERVICE_DATE;
  private static final ZoneId TIME_ZONE = CONSTANTS.TIME_ZONE;

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
