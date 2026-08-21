package org.opentripplanner.updater.trip.gtfs.moduletests.delay;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.time.LocalDate;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.updater.trip.OnEachDstTransitionAndAControlDate;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.gtfs.GtfsRtTestHelper;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * A real-time update should give the same result whether or not it is applied on a service date
 * with a daylight-saving transition - both on a trip that is already in the timetable and on one
 * the message creates. See {@link OnEachDstTransitionAndAControlDate}.
 */
class DstServiceDateTest implements RealtimeTestConstants {

  @OnEachDstTransitionAndAControlDate
  void absoluteTimesOnScheduledTrip(String name, LocalDate serviceDate) {
    var envBuilder = TransitTestEnvironment.of(serviceDate);
    var stopA = envBuilder.stop(STOP_A_ID);
    var stopB = envBuilder.stop(STOP_B_ID);
    var env = envBuilder
      .addTrip(TripInput.of(TRIP_1_ID).addStop(stopA, "10:00").addStop(stopB, "10:10"))
      .build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addStopTime(STOP_A_ID, "10:01")
      .addStopTime(STOP_B_ID, "10:11")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    assertEquals("U | A 10:01 10:01 | B 10:11 10:11", env.tripData(TRIP_1_ID).showTimetable());
  }

  @OnEachDstTransitionAndAControlDate
  void absoluteTimesOnAddedTrip(String name, LocalDate serviceDate) {
    var envBuilder = TransitTestEnvironment.of(serviceDate);
    var stopA = envBuilder.stop(STOP_A_ID);
    var stopB = envBuilder.stop(STOP_B_ID);
    var env = envBuilder
      .addTrip(TripInput.of(TRIP_1_ID).addStop(stopA, "10:00").addStop(stopB, "10:10"))
      .build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdate(ADDED_TRIP_ID, ADDED)
      .addStopTime(STOP_A_ID, "12:01")
      .addStopTime(STOP_B_ID, "12:11")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    var tripTimes = env.tripData(ADDED_TRIP_ID).tripTimes();
    assertEquals(TimeUtils.time("12:01"), tripTimes.getDepartureTime(0));
    assertEquals(TimeUtils.time("12:11"), tripTimes.getArrivalTime(1));
  }
}
