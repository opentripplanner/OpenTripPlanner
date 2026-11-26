package org.opentripplanner.updater.trip.gtfs.moduletests.addition;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.NEW;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.REPLACEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_ARRIVAL_TIME;
import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_DEPARTURE_TIME;
import static org.opentripplanner.updater.spi.UpdateErrorType.TOO_MANY_STOPS;
import static org.opentripplanner.updater.spi.UpdateErrorType.UNKNOWN_STOP;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.updater.spi.UpdateErrorType;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.gtfs.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.gtfs.model.TripUpdate;

public class ReplacementTest implements RealtimeTestConstants {

  private static final long SCHEDULED_TIME = 1_000_000;

  @Test
  void replacementTrip() {
    var builder = TransitTestEnvironment.of();
    var STOP_A = builder.stop(STOP_A_ID);
    var STOP_B = builder.stop(STOP_B_ID);
    builder.stop(STOP_C_ID);
    var TRIP_INPUT = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "8:30:00", "8:30:00")
      .addStop(STOP_B, "8:40:00", "8:40:00")
      .withHeadsign(I18NString.of("Original Headsign"));
    var env = builder.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, REPLACEMENT)
      .withTripProperties(
        "New Headsign",
        // we can't change trip short name at real-time yet
        "SW1234"
      )
      .addStopTime(STOP_A_ID, "00:30")
      .addStopTime(STOP_B_ID, "00:45", "Changed Headsign")
      .addStopTime(STOP_C_ID, "01:00")
      .build();

    rt.applyTripUpdate(tripUpdate);

    // THEN
    var snapshot = env.timetableSnapshot();
    var tripId = id(TRIP_1_ID);

    var transitService = env.transitService();

    // Original trip pattern
    {
      var trip = transitService.getTrip(tripId);
      assertNotNull(trip);
      var originalTripPattern = transitService.findPattern(trip);

      var originalTimetableForToday = snapshot.resolve(
        originalTripPattern,
        env.defaultServiceDate()
      );
      var originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

      assertNotSame(originalTimetableForToday, originalTimetableScheduled);

      var originalTripTimesScheduled = originalTimetableScheduled.getTripTimes(tripId);
      assertNotNull(
        originalTripTimesScheduled,
        "Original trip should be found in scheduled time table"
      );
      assertFalse(
        originalTripTimesScheduled.isCanceledOrDeleted(),
        "Original trip times should not be canceled in scheduled time table"
      );
      assertFalse(originalTripTimesScheduled.hasAnyUpdates());

      var originalTripTimesForToday = originalTimetableForToday.getTripTimes(tripId);
      assertNotNull(
        originalTripTimesForToday,
        "Original trip should be found in time table for service date"
      );
      assertTrue(
        originalTripTimesForToday.isDeleted(),
        "Original trip times should be deleted in time table for service date"
      );
      assertTrue(originalTripTimesForToday.isDeleted());
      assertEquals(I18NString.of("Original Headsign"), trip.getHeadsign());
      assertEquals(
        I18NString.of("Original Headsign"),
        originalTripTimesScheduled.getTripHeadsign()
      );
      assertEquals(I18NString.of("Original Headsign"), originalTripTimesForToday.getTripHeadsign());
      assertEquals(I18NString.of("Original Headsign"), originalTripTimesScheduled.getHeadsign(0));
      assertEquals(I18NString.of("Original Headsign"), originalTripTimesScheduled.getHeadsign(1));
      assertEquals(I18NString.of("Original Headsign"), originalTripTimesForToday.getHeadsign(0));
      assertEquals(I18NString.of("Original Headsign"), originalTripTimesForToday.getHeadsign(1));
    }

    // New trip pattern
    {
      var tripFetcher = env.tripData(TRIP_1_ID);
      assertTrue(tripFetcher.tripTimes().isTripPatternModified());

      var newTripPattern = tripFetcher.tripPattern();
      assertNotNull(newTripPattern, "New trip pattern should be found");

      var tripTimes = tripFetcher.tripTimes();
      var newTimetableScheduled = transitService.findTimetable(newTripPattern, null);

      assertNotNull(tripTimes, "New trip should be found in time table for service date");
      assertTrue(tripTimes.isTripPatternModified());

      assertNull(
        newTimetableScheduled.getTripTimes(tripId),
        "New trip should not be found in scheduled time table"
      );

      assertEquals(I18NString.of("New Headsign"), tripTimes.getTripHeadsign());
      assertEquals(I18NString.of("New Headsign"), tripTimes.getHeadsign(0));
      assertEquals(I18NString.of("Changed Headsign"), tripTimes.getHeadsign(1));
      assertEquals(I18NString.of("New Headsign"), tripTimes.getHeadsign(2));

      assertEquals(
        "P U | A 0:30 0:30 | B 0:45 0:45 | C [EC] 1:00 1:00",
        tripFetcher.showTimetable()
      );
    }
  }

  /**
   * Original A-B-C replaced by A-D-C: the pattern contains only the stops sent by the producer, and
   * D is flagged as an extra call.
   */
  @Test
  void replacementTripFlagsAddedStops() {
    var builder = TransitTestEnvironment.of();
    var STOP_A = builder.stop(STOP_A_ID);
    var STOP_B = builder.stop(STOP_B_ID);
    var STOP_C = builder.stop(STOP_C_ID);
    builder.stop(STOP_D_ID);
    var TRIP_INPUT = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "8:30:00", "8:30:00")
      .addStop(STOP_B, "8:40:00", "8:40:00")
      .addStop(STOP_C, "8:50:00", "8:50:00");
    var env = builder.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, REPLACEMENT)
      .addStopTimeWithScheduled(STOP_A_ID, "8:31", "8:30")
      .addStopTimeWithScheduled(STOP_D_ID, "8:46", "8:45")
      .addStopTimeWithScheduled(STOP_C_ID, "8:56", "8:55")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    assertEquals(
      "P U | A 8:31 8:31 | D [EC] 8:46 8:46 | C 8:56 8:56",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * A stop the producer sends as SKIPPED (A, B skipped, D, C) is shown as canceled. It is matched
   * with the scheduled B, so it is not an extra call.
   */
  @Test
  void replacementTripWithExplicitlySkippedStop() {
    var builder = TransitTestEnvironment.of();
    var STOP_A = builder.stop(STOP_A_ID);
    var STOP_B = builder.stop(STOP_B_ID);
    var STOP_C = builder.stop(STOP_C_ID);
    builder.stop(STOP_D_ID);
    var TRIP_INPUT = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "8:30:00", "8:30:00")
      .addStop(STOP_B, "8:40:00", "8:40:00")
      .addStop(STOP_C, "8:50:00", "8:50:00");
    var env = builder.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, REPLACEMENT)
      .addStopTime(STOP_A_ID, "8:30")
      .addSkippedStop(STOP_B_ID, "8:40")
      .addStopTime(STOP_D_ID, "8:45")
      .addStopTime(STOP_C_ID, "8:55")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    assertEquals(
      "P U | A 8:30 8:30 | B [C] 8:40 8:40 | D [EC] 8:45 8:45 | C 8:55 8:55",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * Original A-B-C-E replaced by A, B skipped, D, C: the producer sends one removed stop as SKIPPED
   * and leaves the other out. B is shown as canceled, D as added, and E is not shown.
   */
  @Test
  void replacementTripWithPartlySkippedStops() {
    var builder = TransitTestEnvironment.of();
    var STOP_A = builder.stop(STOP_A_ID);
    var STOP_B = builder.stop(STOP_B_ID);
    var STOP_C = builder.stop(STOP_C_ID);
    builder.stop(STOP_D_ID);
    var STOP_E = builder.stop(STOP_E_ID);
    var TRIP_INPUT = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "8:30:00", "8:30:00")
      .addStop(STOP_B, "8:40:00", "8:40:00")
      .addStop(STOP_C, "8:50:00", "8:50:00")
      .addStop(STOP_E, "9:00:00", "9:00:00");
    var env = builder.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, REPLACEMENT)
      .addStopTime(STOP_A_ID, "8:30")
      .addSkippedStop(STOP_B_ID, "8:40")
      .addStopTime(STOP_D_ID, "8:45")
      .addStopTime(STOP_C_ID, "8:55")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    assertEquals(
      "P U | A 8:30 8:30 | B [C] 8:40 8:40 | D [EC] 8:45 8:45 | C 8:55 8:55",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * A trip added in real time (A-B-C) and then replaced (A-D-C) has no scheduled trip to compare
   * with, so no stop is flagged as an extra call.
   */
  @Test
  void replacementOfAddedTripFlagsNoStops() {
    var builder = TransitTestEnvironment.of();
    var STOP_A = builder.stop(STOP_A_ID);
    var STOP_B = builder.stop(STOP_B_ID);
    builder.stop(STOP_C_ID);
    builder.stop(STOP_D_ID);
    var TRIP_INPUT = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "8:30:00", "8:30:00")
      .addStop(STOP_B, "8:40:00", "8:40:00");
    var env = builder.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    var addedTrip = rt
      .tripUpdate(ADDED_TRIP_ID, NEW)
      .addStopTime(STOP_A_ID, "9:30")
      .addStopTime(STOP_B_ID, "9:40")
      .addStopTime(STOP_C_ID, "9:50")
      .build();
    var replacement = rt
      .tripUpdate(ADDED_TRIP_ID, REPLACEMENT)
      .addStopTime(STOP_A_ID, "9:30")
      .addStopTime(STOP_D_ID, "9:45")
      .addStopTime(STOP_C_ID, "9:55")
      .build();

    assertSuccess(rt.applyTripUpdates(List.of(addedTrip, replacement)));

    assertEquals(
      "P U | A 9:30 9:30 | D 9:45 9:45 | C 9:55 9:55",
      env.tripData(ADDED_TRIP_ID).showTimetable()
    );
  }

  @Test
  void replacementTripWithUnknownStop() {
    var builder = TransitTestEnvironment.of();
    var STOP_A = builder.stop(STOP_A_ID);
    var STOP_B = builder.stop(STOP_B_ID);
    var TRIP_INPUT = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "8:30:00", "8:30:00")
      .addStop(STOP_B, "8:40:00", "8:40:00");
    var env = builder.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, REPLACEMENT)
      .addStopTime(STOP_A_ID, "00:30")
      .addStopTime("UNKNOWN_STOP_ID", "00:45")
      .addStopTime(STOP_B_ID, "01:00")
      .build();

    assertFailure(UNKNOWN_STOP, rt.applyTripUpdate(tripUpdate));

    var tripTimes = env.tripData(TRIP_1_ID).tripTimes();
    assertNotNull(tripTimes);
    assertFalse(tripTimes.isTripPatternModified());
    assertFalse(tripTimes.isCanceledOrDeleted());
  }

  @Test
  void replacementTripWithTooManyStopsIsRejected() {
    var builder = TransitTestEnvironment.of();
    var STOP_A = builder.stop(STOP_A_ID);
    var STOP_B = builder.stop(STOP_B_ID);
    var TRIP_INPUT = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "8:30:00", "8:30:00")
      .addStop(STOP_B, "8:40:00", "8:40:00");
    var env = builder.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdateBuilder = rt.tripUpdate(TRIP_1_ID, REPLACEMENT);
    for (int i = 0; i <= TripUpdate.MAX_REPLACEMENT_STOPS; i++) {
      tripUpdateBuilder.addStopTime("STOP_" + i, "8:30");
    }

    assertFailure(TOO_MANY_STOPS, rt.applyTripUpdate(tripUpdateBuilder.build()));

    var tripTimes = env.tripData(TRIP_1_ID).tripTimes();
    assertNotNull(tripTimes);
    assertFalse(tripTimes.isTripPatternModified());
    assertFalse(tripTimes.isCanceledOrDeleted());
  }

  @Test
  void replacementTripWithNoDataStop() {
    var env = noDataEnvironment();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, REPLACEMENT)
      .addStopTimeWithScheduled(STOP_A_ID, "8:32", "8:30")
      .addNoDataStopWithScheduled(STOP_B_ID, "8:41")
      .addStopTimeWithScheduled(STOP_C_ID, "8:56", "8:50")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    assertEquals(
      "P U | A 8:32 8:32 | B [ND] 8:41 8:41 | C 8:56 8:56",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  @Test
  void replacementTripWithNoDataExtraCall() {
    var env = noDataEnvironment();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, REPLACEMENT)
      .addStopTimeWithScheduled(STOP_A_ID, "8:30", "8:30")
      .addNoDataStopWithScheduled(STOP_D_ID, "8:45")
      .addStopTimeWithScheduled(STOP_C_ID, "8:50", "8:50")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    assertEquals(
      "P U | A 8:30 8:30 | D [ND,EC] 8:45 8:45 | C 8:50 8:50",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  @Test
  void replacementTripWithNoDataStopAfterDelayedStopIsClampedForwards() {
    var env = noDataEnvironment();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, REPLACEMENT)
      .addStopTimeWithScheduled(STOP_A_ID, "8:45", "8:30")
      .addNoDataStopWithScheduled(STOP_B_ID, "8:40")
      .addStopTimeWithScheduled(STOP_C_ID, "8:55", "8:50")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    assertEquals(
      "P U | A 8:45 8:45 | B [ND] 8:45 8:45 | C 8:55 8:55",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  @Test
  void replacementTripWithNoDataStopBeforeEarlyStopIsClampedBackwards() {
    var env = noDataEnvironment();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, REPLACEMENT)
      .addNoDataStopWithScheduled(STOP_A_ID, "8:30")
      .addStopTimeWithScheduled(STOP_B_ID, "8:25", "8:40")
      .addStopTimeWithScheduled(STOP_C_ID, "8:50", "8:50")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    assertEquals(
      "P U | A [ND] 8:25 8:25 | B 8:25 8:25 | C 8:50 8:50",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  private static Stream<Arguments> invalidNoDataStopCases() {
    var scheduled = StopTimeEvent.newBuilder().setScheduledTime(SCHEDULED_TIME).build();
    return Stream.of(
      Arguments.of(
        "arrival without scheduled time",
        noDataStop()
          .setArrival(StopTimeEvent.newBuilder().setTime(SCHEDULED_TIME))
          .setDeparture(scheduled)
          .build(),
        INVALID_ARRIVAL_TIME
      ),
      Arguments.of(
        "arrival with time",
        noDataStop()
          .setArrival(scheduled.toBuilder().setTime(SCHEDULED_TIME))
          .setDeparture(scheduled)
          .build(),
        INVALID_ARRIVAL_TIME
      ),
      Arguments.of(
        "arrival with delay",
        noDataStop().setArrival(scheduled.toBuilder().setDelay(60)).setDeparture(scheduled).build(),
        INVALID_ARRIVAL_TIME
      ),
      Arguments.of(
        "no departure",
        noDataStop().setArrival(scheduled).build(),
        INVALID_DEPARTURE_TIME
      ),
      Arguments.of(
        "departure with time",
        noDataStop()
          .setArrival(scheduled)
          .setDeparture(scheduled.toBuilder().setTime(SCHEDULED_TIME))
          .build(),
        INVALID_DEPARTURE_TIME
      ),
      Arguments.of(
        "departure with delay",
        noDataStop().setArrival(scheduled).setDeparture(scheduled.toBuilder().setDelay(60)).build(),
        INVALID_DEPARTURE_TIME
      )
    );
  }

  private static StopTimeUpdate.Builder noDataStop() {
    return StopTimeUpdate.newBuilder()
      .setStopId(STOP_B_ID)
      .setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.NO_DATA);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidNoDataStopCases")
  void replacementTripWithInvalidNoDataStopIsRejected(
    String description,
    StopTimeUpdate noDataStop,
    UpdateErrorType expectedError
  ) {
    var env = noDataEnvironment();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, REPLACEMENT)
      .addStopTime(STOP_A_ID, "8:30")
      .addRawStopTime(noDataStop)
      .addStopTime(STOP_C_ID, "8:50")
      .build();

    assertFailure(expectedError, rt.applyTripUpdate(tripUpdate));
    assertScheduledTripUnchanged(env);
  }

  private static TransitTestEnvironment noDataEnvironment() {
    var builder = TransitTestEnvironment.of();
    var STOP_A = builder.stop(STOP_A_ID);
    var STOP_B = builder.stop(STOP_B_ID);
    var STOP_C = builder.stop(STOP_C_ID);
    builder.stop(STOP_D_ID);
    var TRIP_INPUT = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "8:30:00", "8:30:00")
      .addStop(STOP_B, "8:40:00", "8:40:00")
      .addStop(STOP_C, "8:50:00", "8:50:00");
    return builder.addTrip(TRIP_INPUT).build();
  }

  private static void assertScheduledTripUnchanged(TransitTestEnvironment env) {
    var tripTimes = env.tripData(TRIP_1_ID).tripTimes();
    assertNotNull(tripTimes);
    assertFalse(tripTimes.isTripPatternModified());
    assertFalse(tripTimes.isCanceledOrDeleted());
  }
}
