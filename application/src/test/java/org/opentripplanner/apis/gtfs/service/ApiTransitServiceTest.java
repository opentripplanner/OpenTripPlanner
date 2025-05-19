package org.opentripplanner.apis.gtfs.service;

import static com.google.common.truth.Truth.assertThat;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.transit.realtime.GtfsRealtime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

class ApiTransitServiceTest implements RealtimeTestConstants {

  private static final TripInput TRIP1_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A1, "12:00:00", "12:00:00")
    .addStop(STOP_B1, "12:30:00", "12:30:00")
    .addStop(STOP_C1, "13:00:00", "13:00:00")
    .build();

  private static final TripInput TRIP2_INPUT = TripInput.of(TRIP_2_ID)
    .addStop(STOP_A1, "12:10:00", "12:10:00")
    .addStop(STOP_B1, "12:40:00", "12:40:00")
    .addStop(STOP_C1, "13:10:00", "13:10:00")
    .build();

  public static final Instant T11_59 = SERVICE_DATE.atTime(LocalTime.NOON.minusMinutes(1))
    .atZone(TIME_ZONE)
    .toInstant();

  @Test
  void justScheduledTrips() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP1_INPUT).addTrip(TRIP2_INPUT).build();
    var service = new ApiTransitService(env.getTransitService());

    var pattern = env.getPatternForTrip(TRIP_1_ID);
    var calls = service.getTripTimeOnDatesForPatternAtStopIncludingTripsWithSkippedStops(
      STOP_A1,
      pattern,
      T11_59,
      Duration.ofHours(2),
      Integer.MAX_VALUE,
      ArrivalDeparture.BOTH
    );

    assertThat(calls).hasSize(2);
  }

  @Test
  void skipStop() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP1_INPUT).addTrip(TRIP2_INPUT).build();
    var res = env.applyTripUpdates(
      List.of(skipSecondStop(TRIP_1_ID), skipSecondStop(TRIP_2_ID)),
      FULL_DATASET
    );
    assertSuccess(res);
    var transitService = env.getTransitService();
    var service = new ApiTransitService(transitService);

    var trip = transitService.getTrip(id(TRIP_1_ID));
    var scheduledPattern = env.getTransitService().findPattern(trip);
    var calls = service.getTripTimeOnDatesForPatternAtStopIncludingTripsWithSkippedStops(
      STOP_A1,
      scheduledPattern,
      T11_59,
      Duration.ofHours(2),
      Integer.MAX_VALUE,
      ArrivalDeparture.BOTH
    );

    assertThat(calls).hasSize(2);
  }

  private static GtfsRealtime.TripUpdate skipSecondStop(String tripId) {
    return new TripUpdateBuilder(tripId, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addSkippedStop(1)
      .build();
  }
}
