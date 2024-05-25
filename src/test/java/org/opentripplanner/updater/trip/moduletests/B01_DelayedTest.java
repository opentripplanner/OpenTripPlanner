package org.opentripplanner.updater.trip.moduletests;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.siri.RealtimeTestEnvironment;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

public class B01_DelayedTest {


    @Test
    public void delayed() {
      var env = new RealtimeTestEnvironment();

      final TripPattern pattern = env.transitModel.getTransitModelIndex().getPatternForTrip().get(env.trip1);
      final int tripIndex = pattern.getScheduledTimetable().getTripIndex(env.trip1.getId());
      //final int tripIndex2 = pattern.getScheduledTimetable().getTripIndex(env.trip2.getId());

      var tripUpdateBuilder = new TripUpdateBuilder(
        env.trip1.getId().getId(),
        env.serviceDate,
        SCHEDULED,
        env.timeZone
      );

      int stopSequence = 2;
      int delay = 1;
      tripUpdateBuilder.addDelayedStopTime(stopSequence, delay);

      var tripUpdate = tripUpdateBuilder.buildList();


      var result = env.applyTripUpdates(
        tripUpdate
      );

      assertEquals(1, result.successful());

      final TimetableSnapshot snapshot = env.gtfsSource.getTimetableSnapshot();
      final Timetable forToday = snapshot.resolve(pattern, env.serviceDate);
      final Timetable schedule = snapshot.resolve(pattern, null);
      assertNotSame(forToday, schedule);
      assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
      //assertSame(forToday.getTripTimes(tripIndex2), schedule.getTripTimes(tripIndex2));
      assertEquals(1, forToday.getTripTimes(tripIndex).getArrivalDelay(1));
      assertEquals(1, forToday.getTripTimes(tripIndex).getDepartureDelay(1));

      assertEquals(RealTimeState.SCHEDULED, schedule.getTripTimes(tripIndex).getRealTimeState());
      assertEquals(RealTimeState.UPDATED, forToday.getTripTimes(tripIndex).getRealTimeState());

      //assertEquals(RealTimeState.SCHEDULED, schedule.getTripTimes(tripIndex2).getRealTimeState());
      //assertEquals(RealTimeState.SCHEDULED, forToday.getTripTimes(tripIndex2).getRealTimeState());
    }

  }