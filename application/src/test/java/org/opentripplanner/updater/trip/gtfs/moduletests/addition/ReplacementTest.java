package org.opentripplanner.updater.trip.gtfs.moduletests.addition;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.REPLACEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

public class ReplacementTest implements RealtimeTestConstants {

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
        "SW1234" // we can't change trip short name at real-time yet
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
      assertEquals(RealTimeState.SCHEDULED, originalTripTimesScheduled.getRealTimeState());

      var originalTripTimesForToday = originalTimetableForToday.getTripTimes(tripId);
      assertNotNull(
        originalTripTimesForToday,
        "Original trip should be found in time table for service date"
      );
      assertTrue(
        originalTripTimesForToday.isDeleted(),
        "Original trip times should be deleted in time table for service date"
      );
      assertEquals(RealTimeState.DELETED, originalTripTimesForToday.getRealTimeState());
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
      assertEquals(RealTimeState.MODIFIED, tripFetcher.realTimeState());

      var newTripPattern = tripFetcher.tripPattern();
      assertNotNull(newTripPattern, "New trip pattern should be found");

      var tripTimes = tripFetcher.tripTimes();
      var newTimetableScheduled = transitService.findTimetable(newTripPattern, null);

      assertNotNull(tripTimes, "New trip should be found in time table for service date");
      assertEquals(RealTimeState.MODIFIED, tripTimes.getRealTimeState());

      assertNull(
        newTimetableScheduled.getTripTimes(tripId),
        "New trip should not be found in scheduled time table"
      );

      assertEquals(I18NString.of("New Headsign"), tripTimes.getTripHeadsign());
      assertEquals(I18NString.of("New Headsign"), tripTimes.getHeadsign(0));
      assertEquals(I18NString.of("Changed Headsign"), tripTimes.getHeadsign(1));
      assertEquals(I18NString.of("New Headsign"), tripTimes.getHeadsign(2));
    }
  }
}
