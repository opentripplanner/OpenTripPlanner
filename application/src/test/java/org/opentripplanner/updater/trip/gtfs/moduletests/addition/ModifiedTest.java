package org.opentripplanner.updater.trip.gtfs.moduletests.addition;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.REPLACEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_A;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_B;
import static org.opentripplanner.updater.trip.gtfs.moduletests.addition.AddedTest.assertAddedTrip;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

public class ModifiedTest implements RealtimeTestConstants {

  @Test
  void modifiedTrip() {
    var TRIP_INPUT = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "8:30:00", "8:30:00")
      .addStop(STOP_B, "8:40:00", "8:40:00")
      .withHeadsign("Original Headsign")
      .build();
    var env = RealtimeTestEnvironment.of()
      .withStops(STOP_A_ID, STOP_B_ID, STOP_C_ID, STOP_D_ID)
      .addTrip(TRIP_INPUT)
      .build();
    var builder = new TripUpdateBuilder(
      TRIP_1_ID,
      SERVICE_DATE,
      REPLACEMENT,
      TIME_ZONE,
      "New Headsign",
      "SW1234" // we can't change trip short name at real-time yet
    );
    builder
      .addStopTime(STOP_A_ID, 30)
      .addStopTime(STOP_B_ID, 45, "Changed Headsign")
      .addStopTime(STOP_C_ID, 60);

    var tripUpdate = builder.build();

    env.applyTripUpdate(tripUpdate);

    // THEN
    var snapshot = env.getTimetableSnapshot();
    var tripId = TimetableRepositoryForTest.id(TRIP_1_ID);

    var transitService = env.getTransitService();
    // We do not support trip headsign by service date
    // TODO: I currently have no idea how TripOnServiceDate will behave, and will need to revisit this after #5393 is merged
    assertEquals(
      I18NString.of("Original Headsign"),
      transitService.getTrip(TimetableRepositoryForTest.id(TRIP_1_ID)).getHeadsign()
    );

    // Original trip pattern
    {
      var trip = transitService.getTrip(tripId);
      var originalTripPattern = transitService.findPattern(trip);

      var originalTimetableForToday = snapshot.resolve(originalTripPattern, SERVICE_DATE);
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
      assertEquals(I18NString.of("Original Headsign"), originalTripTimesScheduled.getHeadsign(0));
      assertEquals(I18NString.of("Original Headsign"), originalTripTimesScheduled.getHeadsign(1));
      assertEquals(I18NString.of("Original Headsign"), originalTripTimesForToday.getHeadsign(0));
      assertEquals(I18NString.of("Original Headsign"), originalTripTimesForToday.getHeadsign(1));
    }

    // New trip pattern
    {
      assertAddedTrip(TRIP_1_ID, env, RealTimeState.MODIFIED);
      var newTripPattern = snapshot.getNewTripPatternForModifiedTrip(tripId, SERVICE_DATE);
      assertNotNull(newTripPattern, "New trip pattern should be found");

      var newTimetableForToday = snapshot.resolve(newTripPattern, SERVICE_DATE);
      var newTimetableScheduled = snapshot.resolve(newTripPattern, null);

      assertNotSame(newTimetableForToday, newTimetableScheduled);

      var tripTimes = newTimetableForToday.getTripTimes(tripId);
      assertNotNull(tripTimes, "New trip should be found in time table for service date");
      assertEquals(RealTimeState.MODIFIED, tripTimes.getRealTimeState());

      assertNull(
        newTimetableScheduled.getTripTimes(tripId),
        "New trip should not be found in scheduled time table"
      );

      assertEquals(I18NString.of("New Headsign"), tripTimes.getHeadsign(0));
      assertEquals(I18NString.of("Changed Headsign"), tripTimes.getHeadsign(1));
      assertEquals(I18NString.of("New Headsign"), tripTimes.getHeadsign(2));
    }
  }
}
