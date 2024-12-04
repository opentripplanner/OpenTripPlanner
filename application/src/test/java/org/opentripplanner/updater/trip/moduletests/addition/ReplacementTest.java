package org.opentripplanner.updater.trip.moduletests.addition;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.REPLACEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.trip.moduletests.addition.AddedTest.assertAddedTrip;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

public class ReplacementTest implements RealtimeTestConstants {

  @Test
  void modifiedTrip() {
    var TRIP_INPUT = TripInput
      .of(TRIP_1_ID)
      .addStop(STOP_A1, "8:30:00", "8:30:00")
      .addStop(STOP_B1, "8:40:00", "8:40:00")
      .withHeadsign("Original Headsign")
      .build();
    var env = RealtimeTestEnvironment.gtfs().addTrip(TRIP_INPUT).build();
    var builder = new TripUpdateBuilder(
      TRIP_1_ID,
      SERVICE_DATE,
      REPLACEMENT,
      TIME_ZONE,
      "New Headsign"
    );
    builder
      .addStopTime(STOP_A1_ID, 30)
      .addStopTime(STOP_B1_ID, 45, "Changed Headsign")
      .addStopTime(STOP_C1_ID, 60);

    var tripUpdate = builder.build();

    env.applyTripUpdate(tripUpdate);

    // THEN
    final TripPattern tripPattern = assertAddedTrip(TRIP_1_ID, env, RealTimeState.MODIFIED);
    var snapshot = env.getTimetableSnapshot();
    var forToday = snapshot.resolve(tripPattern, SERVICE_DATE);
    var forTodayAddedTripIndex = forToday.getTripIndex(TRIP_1_ID);
    var tripTimes = forToday.getTripTimes(forTodayAddedTripIndex);
    final FeedScopedId tripId = TimetableRepositoryForTest.id(TRIP_1_ID);

    // Original trip pattern
    TransitService transitService = env.getTransitService();
    {
      final Trip trip = transitService.getTrip(tripId);
      final TripPattern originalTripPattern = transitService.findPattern(trip);

      final Timetable originalTimetableForToday = snapshot.resolve(
        originalTripPattern,
        SERVICE_DATE
      );
      final Timetable originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

      assertNotSame(originalTimetableForToday, originalTimetableScheduled);

      final int originalTripIndexScheduled = originalTimetableScheduled.getTripIndex(
        TRIP_1_ID
      );
      assertTrue(
        originalTripIndexScheduled > -1,
        "Original trip should be found in scheduled time table"
      );
      final TripTimes originalTripTimesScheduled = originalTimetableScheduled.getTripTimes(
        originalTripIndexScheduled
      );
      assertFalse(
        originalTripTimesScheduled.isCanceledOrDeleted(),
        "Original trip times should not be canceled in scheduled time table"
      );
      assertEquals(RealTimeState.SCHEDULED, originalTripTimesScheduled.getRealTimeState());

      final int originalTripIndexForToday = originalTimetableForToday.getTripIndex(TRIP_1_ID);
      assertTrue(
        originalTripIndexForToday > -1,
        "Original trip should be found in time table for service date"
      );
      final TripTimes originalTripTimesForToday = originalTimetableForToday.getTripTimes(
        originalTripIndexForToday
      );
      assertTrue(
        originalTripTimesForToday.isDeleted(),
        "Original trip times should be deleted in time table for service date"
      );
      assertEquals(RealTimeState.DELETED, originalTripTimesForToday.getRealTimeState());
    }

    // New trip pattern
    {
      final TripPattern newTripPattern = snapshot.getNewTripPatternForModifiedTrip(
        tripId,
        SERVICE_DATE
      );
      assertNotNull(newTripPattern, "New trip pattern should be found");

      final Timetable newTimetableForToday = snapshot.resolve(newTripPattern, SERVICE_DATE);
      final Timetable newTimetableScheduled = snapshot.resolve(newTripPattern, null);

      assertNotSame(newTimetableForToday, newTimetableScheduled);

      final int newTimetableForTodayModifiedTripIndex = newTimetableForToday.getTripIndex(
        TRIP_1_ID
      );
      assertTrue(
        newTimetableForTodayModifiedTripIndex > -1,
        "New trip should be found in time table for service date"
      );
      assertEquals(
        RealTimeState.MODIFIED,
        newTimetableForToday.getTripTimes(newTimetableForTodayModifiedTripIndex).getRealTimeState()
      );

      assertEquals(
        -1,
        newTimetableScheduled.getTripIndex(TRIP_1_ID),
        "New trip should not be found in scheduled time table"
      );
    }


    // We do not support trip headsign by service date
    // TODO: I currently have no idea how TripOnServiceDate will behave, and will need to revisit this after #5393 is merged
    assertEquals(
      I18NString.of("Original Headsign"),
      transitService.getTrip(TimetableRepositoryForTest.id(TRIP_1_ID)).getHeadsign()
    );
    assertEquals(I18NString.of("New Headsign"), tripTimes.getHeadsign(0));
    assertEquals(I18NString.of("Changed Headsign"), tripTimes.getHeadsign(1));
    assertEquals(I18NString.of("New Headsign"), tripTimes.getHeadsign(2));
  }
}
