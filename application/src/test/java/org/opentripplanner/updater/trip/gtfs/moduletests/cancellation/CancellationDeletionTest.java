package org.opentripplanner.updater.trip.gtfs.moduletests.cancellation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

/**
 * Cancellations and deletions should end up in the internal data model and make trips unavailable
 * for routing.
 */
class CancellationDeletionTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop STOP_A = envBuilder.stop(STOP_A_ID);
  private final RegularStop STOP_B = envBuilder.stop(STOP_B_ID);

  static List<Arguments> cases() {
    return List.of(
      Arguments.of(ScheduleRelationship.CANCELED, RealTimeState.CANCELED),
      Arguments.of(ScheduleRelationship.DELETED, RealTimeState.DELETED)
    );
  }

  @ParameterizedTest
  @MethodSource("cases")
  void cancelledTrip(ScheduleRelationship relationship, RealTimeState state) {
    var env = envBuilder
      .addTrip(
        TripInput.of(TRIP_1_ID)
          .addStop(STOP_A, "0:00:10", "0:00:11")
          .addStop(STOP_B, "0:00:20", "0:00:21")
      )
      .build();
    var rt = GtfsRtTestHelper.of(env);

    var update = rt.tripUpdate(TRIP_1_ID, relationship).build();
    assertSuccess(rt.applyTripUpdate(update));

    var forToday = env.tripData(TRIP_1_ID).tripTimes();
    var schedule = env.tripData(TRIP_1_ID).scheduledTripTimes();
    assertNotSame(forToday, schedule);

    assertEquals(state, forToday.getRealTimeState());
    assertTrue(forToday.isCanceledOrDeleted());
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
    // just to set the scheduling period
    var env = envBuilder
      .addTrip(
        TripInput.of(TRIP_1_ID)
          // just to set the scheduling period
          .withServiceDates(envBuilder.defaultServiceDate())
          .addStop(STOP_A, "0:00:10", "0:00:11")
          .addStop(STOP_B, "0:00:20", "0:00:21")
      )
      .build();
    var rt = GtfsRtTestHelper.of(env);
    var addedTripId = "added-trip";
    // First add ADDED trip
    var update = rt
      .tripUpdate(addedTripId, ScheduleRelationship.ADDED)
      .addStopTime(STOP_A_ID, "00:30")
      .addStopTime(STOP_B_ID, "00:40")
      .addStopTime(STOP_C_ID, "00:55")
      .build();

    assertSuccess(rt.applyTripUpdate(update, DIFFERENTIAL));

    // Cancel or delete the added trip
    update = rt.tripUpdate(addedTripId, relationship).build();
    assertSuccess(rt.applyTripUpdate(update, DIFFERENTIAL));

    var snapshot = env.timetableSnapshot();
    // Get the trip pattern of the added trip which goes through stopA
    var patternsAtA = snapshot.getPatternsForStop(STOP_A);

    assertNotNull(patternsAtA, "Added trip pattern should be found");
    var tripPattern = patternsAtA.stream().findFirst().get();

    var forToday = snapshot.resolve(tripPattern, env.defaultServiceDate());
    var schedule = snapshot.resolve(tripPattern, null);

    assertNotSame(forToday, schedule);

    var realtimeTripTimes = forToday.getTripTimes(id(addedTripId));
    assertNotNull(
      realtimeTripTimes,
      "Added trip should be found in time table for the service date"
    );
    assertEquals(state, realtimeTripTimes.getRealTimeState());

    var scheduledTripTimes = schedule.getTripTimes(id(addedTripId));
    assertNull(scheduledTripTimes, "Added trip should not be found in scheduled time table");
  }
}
