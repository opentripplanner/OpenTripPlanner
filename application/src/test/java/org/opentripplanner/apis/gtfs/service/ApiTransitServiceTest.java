package org.opentripplanner.apis.gtfs.service;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.transit.realtime.GtfsRealtime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * This test uses the real-time test environment to establish the condition under test.
 * This pulls in a bunch of dependencies, namely on GTFS-RT classes, that go a bit beyond the scope
 * of this test, however, the other option is to write to the timetable snapshot/repository directly.
 * In a dev meeting this was considered "white box testing" and worse than pulling these dependencies.
 * <p>
 * The core problem is that OTP doesn't have a clear internal API for applying real-time updates.
 */
class ApiTransitServiceTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder envBuilder = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A = envBuilder.stop(STOP_A_ID);
  private final RegularStop STOP_B = envBuilder.stop(STOP_B_ID);
  private final RegularStop STOP_C = envBuilder.stop(STOP_C_ID);

  private final TripInput TRIP1_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A, "12:00:00", "12:00:00")
    .addStop(STOP_B, "12:30:00", "12:30:00")
    .addStop(STOP_C, "13:00:00", "13:00:00")
    .build();

  private final TripInput TRIP2_INPUT = TripInput.of(TRIP_2_ID)
    .addStop(STOP_A, "12:10:00", "12:10:00")
    .addStop(STOP_B, "12:40:00", "12:40:00")
    .addStop(STOP_C, "13:10:00", "13:10:00")
    .build();

  public final Instant T11_59 = SERVICE_DATE.atTime(LocalTime.NOON.minusMinutes(1))
    .atZone(TIME_ZONE)
    .toInstant();

  @Test
  void justScheduledTrips() {
    var env = envBuilder.addTrip(TRIP1_INPUT).addTrip(TRIP2_INPUT).build();
    var service = new ApiTransitService(env.getTransitService());

    var pattern = env.getPatternForTrip(TRIP_1_ID);
    var calls = service.getTripTimeOnDatesForPatternAtStopIncludingTripsWithSkippedStops(
      STOP_A,
      pattern,
      T11_59,
      Duration.ofHours(2),
      Integer.MAX_VALUE,
      ArrivalDeparture.BOTH
    );

    var tripIds = calls.stream().map(t -> t.getTrip().getId().getId()).toList();
    assertEquals(List.of(TRIP_1_ID, TRIP_2_ID), tripIds);
  }

  /**
   * Tests that you get a single {@link TripTimeOnDate} for a stop in a pattern even if several
   * trips in the pattern have the same stop skipped.
   *
   * @see https://github.com/opentripplanner/OpenTripPlanner/issues/6654
   */
  @Test
  void skipStopInMultipleTripsInPattern() {
    var env = envBuilder.addTrip(TRIP1_INPUT).addTrip(TRIP2_INPUT).build();
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
      STOP_A,
      scheduledPattern,
      T11_59,
      Duration.ofHours(2),
      Integer.MAX_VALUE,
      ArrivalDeparture.BOTH
    );

    var tripIds = calls.stream().map(t -> t.getTrip().getId().getId()).toList();
    assertEquals(List.of(TRIP_1_ID, TRIP_2_ID), tripIds);
  }

  private static GtfsRealtime.TripUpdate skipSecondStop(String tripId) {
    return new TripUpdateBuilder(tripId, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addSkippedStop(1)
      .build();
  }
}
