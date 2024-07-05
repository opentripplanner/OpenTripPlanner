package org.opentripplanner.updater.trip.moduletests.cancellation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * Cancellations and deletions should end up in the internal data model and make trips unavailable
 * for routing.
 */
public class CancellationDeletionTest {

  static List<Arguments> cases() {
    return List.of(
      Arguments.of(ScheduleRelationship.CANCELED, RealTimeState.CANCELED),
      Arguments.of(ScheduleRelationship.DELETED, RealTimeState.DELETED)
    );
  }

  @ParameterizedTest
  @MethodSource("cases")
  public void cancelledTrip(ScheduleRelationship relationship, RealTimeState state) {
    var env = RealtimeTestEnvironment.gtfs();
    var pattern1 = env.getPatternForTrip(env.trip1);

    final int tripIndex1 = pattern1.getScheduledTimetable().getTripIndex(env.trip1.getId());

    var update = new TripUpdateBuilder(
      env.trip1.getId().getId(),
      RealtimeTestEnvironment.SERVICE_DATE,
      relationship,
      env.timeZone
    )
      .build();
    var result = env.applyTripUpdate(update);

    assertEquals(1, result.successful());

    final TimetableSnapshot snapshot = env.getTimetableSnapshot();
    final Timetable forToday = snapshot.resolve(pattern1, RealtimeTestEnvironment.SERVICE_DATE);
    final Timetable schedule = snapshot.resolve(pattern1, null);
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
  public void cancelingAddedTrip(ScheduleRelationship relationship, RealTimeState state) {
    var env = RealtimeTestEnvironment.gtfs();
    var addedTripId = "added-trip";
    // First add ADDED trip
    var update = new TripUpdateBuilder(
      addedTripId,
      RealtimeTestEnvironment.SERVICE_DATE,
      ScheduleRelationship.ADDED,
      env.timeZone
    )
      .addStopTime(env.stopA1.getId().getId(), 30)
      .addStopTime(env.stopB1.getId().getId(), 40)
      .addStopTime(env.stopC1.getId().getId(), 55)
      .build();

    var result = env.applyTripUpdate(update, DIFFERENTIAL);

    assertEquals(1, result.successful());

    // Cancel or delete the added trip
    update =
      new TripUpdateBuilder(
        addedTripId,
        RealtimeTestEnvironment.SERVICE_DATE,
        relationship,
        env.timeZone
      )
        .build();
    result = env.applyTripUpdate(update, DIFFERENTIAL);

    assertEquals(1, result.successful());

    final TimetableSnapshot snapshot = env.getTimetableSnapshot();
    // Get the trip pattern of the added trip which goes through stopA
    var patternsAtA = snapshot.getPatternsForStop(env.stopA1);

    assertNotNull(patternsAtA, "Added trip pattern should be found");
    var tripPattern = patternsAtA.stream().findFirst().get();

    final Timetable forToday = snapshot.resolve(tripPattern, RealtimeTestEnvironment.SERVICE_DATE);
    final Timetable schedule = snapshot.resolve(tripPattern, null);

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
