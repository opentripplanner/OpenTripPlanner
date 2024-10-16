package org.opentripplanner.updater.trip.moduletests.cancellation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * Cancellations and deletions should end up in the internal data model and make trips unavailable
 * for routing.
 */
public class CancellationDeletionTest implements RealtimeTestConstants {

  static List<Arguments> cases() {
    return List.of(
      Arguments.of(ScheduleRelationship.CANCELED, RealTimeState.CANCELED),
      Arguments.of(ScheduleRelationship.DELETED, RealTimeState.DELETED)
    );
  }

  @ParameterizedTest
  @MethodSource("cases")
  void cancelledTrip(ScheduleRelationship relationship, RealTimeState state) {
    var env = RealtimeTestEnvironment
      .gtfs()
      .addTrip(
        TripInput
          .of(TRIP_1_ID)
          .addStop(STOP_A1, "0:00:10", "0:00:11")
          .addStop(STOP_B1, "0:00:20", "0:00:21")
          .build()
      )
      .build();
    var pattern1 = env.getPatternForTrip(TRIP_1_ID);

    final int tripIndex1 = pattern1.getScheduledTimetable().getTripIndex(id(TRIP_1_ID));

    var update = new TripUpdateBuilder(TRIP_1_ID, SERVICE_DATE, relationship, TIME_ZONE).build();
    assertSuccess(env.applyTripUpdate(update));

    var snapshot = env.getTimetableSnapshot();
    var forToday = snapshot.resolve(pattern1, SERVICE_DATE);
    var schedule = snapshot.resolve(pattern1, null);
    assertNotSame(forToday, schedule);
    assertNotSame(forToday.getTripTimes(tripIndex1), schedule.getTripTimes(tripIndex1));

    var tripTimes = forToday.getTripTimes(tripIndex1);

    assertEquals(state, tripTimes.getRealTimeState());
    assertTrue(tripTimes.isCanceledOrDeleted());
  }

  /**
   * Test behavior of the realtime system in a case related to #5725 that is discussed at:
   * https://github.com/opentripplanner/OpenTripPlanner/pull/5726#discussion_r1521653840
   * When a trip is added by a realtime message, in the realtime data indexes a corresponding
   * trip pattern should be associated with the stops that trip visits. When a subsequent
   * realtime message cancels or deletes that trip, the pattern should continue to be present in
   * the realtime data indexes, and it should still contain the previously added trip, but that
   * trip should be marked as having canceled or deleted status. At no point should the trip
   * added by realtime data be present in the trip pattern for scheduled service.
   */
  @ParameterizedTest
  @MethodSource("cases")
  void cancelingAddedTrip(ScheduleRelationship relationship, RealTimeState state) {
    var env = RealtimeTestEnvironment.gtfs().build();
    var addedTripId = "added-trip";
    // First add ADDED trip
    var update = new TripUpdateBuilder(
      addedTripId,
      SERVICE_DATE,
      ScheduleRelationship.ADDED,
      TIME_ZONE
    )
      .addStopTime(STOP_A1_ID, 30)
      .addStopTime(STOP_B1_ID, 40)
      .addStopTime(STOP_C1_ID, 55)
      .build();

    assertSuccess(env.applyTripUpdate(update, DIFFERENTIAL));

    // Cancel or delete the added trip
    update = new TripUpdateBuilder(addedTripId, SERVICE_DATE, relationship, TIME_ZONE).build();
    assertSuccess(env.applyTripUpdate(update, DIFFERENTIAL));

    var snapshot = env.getTimetableSnapshot();
    // Get the trip pattern of the added trip which goes through stopA
    var patternsAtA = snapshot.getPatternsForStop(STOP_A1);

    assertNotNull(patternsAtA, "Added trip pattern should be found");
    var tripPattern = patternsAtA.stream().findFirst().get();

    var forToday = snapshot.resolve(tripPattern, SERVICE_DATE);
    var schedule = snapshot.resolve(tripPattern, null);

    assertNotSame(forToday, schedule);

    final int forTodayAddedTripIndex = forToday.getTripIndex(addedTripId);
    assertTrue(
      forTodayAddedTripIndex > -1,
      "Added trip should be found in time table for the service date"
    );
    assertEquals(state, forToday.getTripTimes(forTodayAddedTripIndex).getRealTimeState());

    final int scheduleTripIndex = schedule.getTripIndex(addedTripId);
    assertEquals(-1, scheduleTripIndex, "Added trip should not be found in scheduled time table");
  }
}
