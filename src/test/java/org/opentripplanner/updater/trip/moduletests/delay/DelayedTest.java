package org.opentripplanner.updater.trip.moduletests.delay;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * Delays should be applied to the first trip but should leave the second trip untouched.
 */
public class DelayedTest {

  private static final int DELAY = 1;
  private static final int STOP_SEQUENCE = 1;

  @Test
  public void delayed() {
    var env = RealtimeTestEnvironment.gtfs();

    var tripUpdate = new TripUpdateBuilder(
      env.trip1.getId().getId(),
      RealtimeTestEnvironment.SERVICE_DATE,
      SCHEDULED,
      env.timeZone
    )
      .addDelayedStopTime(STOP_SEQUENCE, DELAY)
      .build();

    var result = env.applyTripUpdate(tripUpdate);

    assertEquals(1, result.successful());

    // trip1 should be modified
    {
      var pattern1 = env.getPatternForTrip(env.trip1);
      final int trip1Index = pattern1.getScheduledTimetable().getTripIndex(env.trip1.getId());

      final TimetableSnapshot snapshot = env.getTimetableSnapshot();
      final Timetable trip1Realtime = snapshot.resolve(
        pattern1,
        RealtimeTestEnvironment.SERVICE_DATE
      );
      final Timetable trip1Scheduled = snapshot.resolve(pattern1, null);

      assertNotSame(trip1Realtime, trip1Scheduled);
      assertNotSame(
        trip1Realtime.getTripTimes(trip1Index),
        trip1Scheduled.getTripTimes(trip1Index)
      );
      assertEquals(1, trip1Realtime.getTripTimes(trip1Index).getArrivalDelay(STOP_SEQUENCE));
      assertEquals(1, trip1Realtime.getTripTimes(trip1Index).getDepartureDelay(STOP_SEQUENCE));

      assertEquals(
        RealTimeState.SCHEDULED,
        trip1Scheduled.getTripTimes(trip1Index).getRealTimeState()
      );
      assertEquals(
        RealTimeState.UPDATED,
        trip1Realtime.getTripTimes(trip1Index).getRealTimeState()
      );
    }

    // trip2 should keep the scheduled information
    {
      var pattern = env.getPatternForTrip(env.trip2);
      final int tripIndex = pattern.getScheduledTimetable().getTripIndex(env.trip2.getId());

      final TimetableSnapshot snapshot = env.getTimetableSnapshot();
      final Timetable realtime = snapshot.resolve(pattern, RealtimeTestEnvironment.SERVICE_DATE);
      final Timetable scheduled = snapshot.resolve(pattern, null);

      assertSame(realtime, scheduled);
      assertSame(realtime.getTripTimes(tripIndex), scheduled.getTripTimes(tripIndex));
      assertEquals(0, realtime.getTripTimes(tripIndex).getArrivalDelay(STOP_SEQUENCE));
      assertEquals(0, realtime.getTripTimes(tripIndex).getDepartureDelay(STOP_SEQUENCE));

      assertEquals(RealTimeState.SCHEDULED, scheduled.getTripTimes(tripIndex).getRealTimeState());
      assertEquals(RealTimeState.SCHEDULED, realtime.getTripTimes(tripIndex).getRealTimeState());
    }
  }
}
