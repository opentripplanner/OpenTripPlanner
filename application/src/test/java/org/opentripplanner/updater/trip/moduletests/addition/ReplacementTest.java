package org.opentripplanner.updater.trip.moduletests.addition;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.REPLACEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.trip.moduletests.addition.AddedTest.assertAddedTrip;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

public class ReplacementTest implements RealtimeTestConstants {

  @Test
  void replacementTripShouldReplaceHeadsignIfSpecified() {
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

    // We do not support trip headsign by service date
    // TODO: I currently have no idea how TripOnServiceDate will behave, and will need to revisit this after #5393 is merged
    assertEquals(
      I18NString.of("Original Headsign"),
      env.getTransitService().getTrip(TimetableRepositoryForTest.id(TRIP_1_ID)).getHeadsign()
    );
    assertEquals(I18NString.of("New Headsign"), tripTimes.getHeadsign(0));
    assertEquals(I18NString.of("Changed Headsign"), tripTimes.getHeadsign(1));
    assertEquals(I18NString.of("New Headsign"), tripTimes.getHeadsign(2));
  }
}
