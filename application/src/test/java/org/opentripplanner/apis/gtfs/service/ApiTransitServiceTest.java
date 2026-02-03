package org.opentripplanner.apis.gtfs.service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.transit.realtime.GtfsRealtime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * This test uses the real-time test environment to establish the condition under test.
 * This pulls in a bunch of dependencies, namely on GTFS-RT classes, that go a bit beyond the scope
 * of this test, however, the other option is to write to the timetable snapshot/repository directly.
 * In a dev meeting this was considered "white box testing" and worse than pulling these dependencies.
 * <p>
 * The core problem is that OTP doesn't have a clear internal API for applying real-time updates.
 */
class ApiTransitServiceTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 5, 8);
  private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Paris");
  private static final ZonedDateTime ANY_TIME = ZonedDateTime.parse("2022-01-01T12:00:00+00:00");
  private static final String TRIP_1_ID = "TestTrip1";
  private static final String TRIP_2_ID = "TestTrip2";

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(SERVICE_DATE);
  private final RegularStop STOP_A = envBuilder.stop("A");
  private final RegularStop STOP_B = envBuilder.stop("B");
  private final RegularStop STOP_C = envBuilder.stop("C");

  private final TripInput TRIP1_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A, "12:00:00", "12:00:00")
    .addStop(STOP_B, "12:30:00", "12:30:00")
    .addStop(STOP_C, "13:00:00", "13:00:00");

  private final TripInput TRIP2_INPUT = TripInput.of(TRIP_2_ID)
    .addStop(STOP_A, "12:10:00", "12:10:00")
    .addStop(STOP_B, "12:40:00", "12:40:00")
    .addStop(STOP_C, "13:10:00", "13:10:00");

  public final Instant T11_59 = SERVICE_DATE.atTime(LocalTime.NOON.minusMinutes(1))
    .atZone(TIME_ZONE)
    .toInstant();

  @Test
  void justScheduledTrips() {
    var env = envBuilder.addTrip(TRIP1_INPUT).addTrip(TRIP2_INPUT).build();
    var service = new ApiTransitService(env.transitService());

    var pattern = env.tripData(TRIP_1_ID).tripPattern();
    var calls = service.getTripTimeOnDatesForPatternAtStopIncludingTripsWithSkippedStops(
      STOP_A,
      pattern,
      T11_59,
      Duration.ofHours(2),
      Integer.MAX_VALUE,
      ArrivalDeparture.BOTH
    );

    var tripIds = calls
      .stream()
      .map(t -> t.getTrip().getId().getId())
      .toList();
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
    var rt = GtfsRtTestHelper.of(env);

    var res = rt.applyTripUpdates(
      List.of(
        skipSecondStop(rt.tripUpdateScheduled(TRIP_1_ID)),
        skipSecondStop(rt.tripUpdateScheduled(TRIP_2_ID))
      ),
      FULL_DATASET
    );
    assertSuccess(res);
    var transitService = env.transitService();
    var service = new ApiTransitService(transitService);

    var trip = transitService.getTrip(id(TRIP_1_ID));
    var scheduledPattern = env.transitService().findPattern(trip);
    var calls = service.getTripTimeOnDatesForPatternAtStopIncludingTripsWithSkippedStops(
      STOP_A,
      scheduledPattern,
      T11_59,
      Duration.ofHours(2),
      Integer.MAX_VALUE,
      ArrivalDeparture.BOTH
    );

    var tripIds = calls
      .stream()
      .map(t -> t.getTrip().getId().getId())
      .toList();
    assertEquals(List.of(TRIP_1_ID, TRIP_2_ID), tripIds);
  }

  @Test
  void transitLegCalls() {
    var env = envBuilder.addTrip(TRIP1_INPUT).build();
    var service = new ApiTransitService(env.transitService());

    var tripData = env.tripData(TRIP_1_ID);
    var tripTimes = tripData.tripTimes();
    var pattern = tripData.tripPattern();

    var leg = new ScheduledTransitLegBuilder()
      .withTripPattern(pattern)
      .withTripTimes(tripTimes)
      .withStartTime(ANY_TIME)
      .withEndTime(ANY_TIME)
      .withServiceDate(SERVICE_DATE)
      .withZoneId(env.timeZone())
      .withDistanceMeters(1000)
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(2)
      .build();
    var calls = service.findStopCalls(leg);
    assertEquals(
      "[" +
        "TripTimeOnDate{trip: Trip{F:TestTrip1 RRoute1}, stopPosition: 0, arrival: 12:00, departure: 12:00, serviceDate: 2024-05-08}, " +
        "TripTimeOnDate{trip: Trip{F:TestTrip1 RRoute1}, stopPosition: 1, arrival: 12:30, departure: 12:30, serviceDate: 2024-05-08}, " +
        "TripTimeOnDate{trip: Trip{F:TestTrip1 RRoute1}, stopPosition: 2, arrival: 13:00, departure: 13:00, serviceDate: 2024-05-08}" +
        "]",
      calls.toString()
    );
  }

  @Test
  void streetLegCalls() {
    var env = envBuilder.addTrip(TRIP1_INPUT).build();
    var service = new ApiTransitService(env.transitService());

    var leg = StreetLeg.of()
      .withMode(TraverseMode.WALK)
      .withStartTime(ANY_TIME)
      .withEndTime(ANY_TIME)
      .withDistanceMeters(1000)
      .build();
    var calls = service.findStopCalls(leg);
    assertThat(calls).isEmpty();
  }

  private static GtfsRealtime.TripUpdate skipSecondStop(TripUpdateBuilder builder) {
    return builder.addSkippedStop(1).build();
  }
}
