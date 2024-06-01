package org.opentripplanner.updater.trip.moduletests.cancellation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
