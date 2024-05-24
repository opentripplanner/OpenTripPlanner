package org.opentripplanner.updater.trip.moduletests;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.siri.RealtimeTestEnvironment;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

public class C01_CancellationTest {

  @Test
  public void cancelledTrip() {
    var env = new RealtimeTestEnvironment();
    var pattern = env.transitModel
      .getTransitModelIndex()
      .getPatternForTrip()
      .get(env.trip1);
    final int tripIndex = pattern.getScheduledTimetable().getTripIndex(env.trip1.getId());
    final int tripIndex2 = pattern.getScheduledTimetable().getTripIndex(env.trip2.getId());

    var cancellation = new TripUpdateBuilder(
      env.trip1.getId().getId(),
      env.serviceDate,
      CANCELED,
      env.timeZone
    )
      .build();
    var result = env.applyTripUpdates(List.of(cancellation));

    assertEquals(1, result.successful());

    final TimetableSnapshot snapshot = env.gtfsSource.getTimetableSnapshot();
    final Timetable forToday = snapshot.resolve(pattern, env.serviceDate);
    final Timetable schedule = snapshot.resolve(pattern, null);
    assertNotSame(forToday, schedule);
    assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
    //assertSame(forToday.getTripTimes(tripIndex2), schedule.getTripTimes(tripIndex2));

    var tripTimes = forToday.getTripTimes(tripIndex);

    assertEquals(RealTimeState.CANCELED, tripTimes.getRealTimeState());
  }
}
