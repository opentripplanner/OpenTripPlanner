package org.opentripplanner.updater.trip.gtfs.moduletests.rejection;

import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_REFERENCE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

class InvalidStopRefTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder builder = TransitTestEnvironment.of();
  private final RegularStop stopA = builder.stop(STOP_A_ID);
  private final RegularStop stopB = builder.stop(STOP_B_ID);
  private final TripInput tripInput = TripInput.of(TRIP_1_ID)
    .addStop(stopA, "10:00", "10:00")
    .addStop(stopB, "10:10", "10:10");

  @Test
  void unknownStopId() {
    var env = builder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);
    var update = rt.tripUpdateScheduled(TRIP_1_ID).addStopTime("unknown stop", "10:00").build();

    assertFailure(INVALID_STOP_REFERENCE, rt.applyTripUpdate(update));
  }

  @Test
  void knownAndUnknownStopId() {
    var env = builder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);
    var update = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addStopTime(STOP_A_ID, "10:00")
      .addStopTime("unknown stop", "10:00")
      .build();

    assertFailure(INVALID_STOP_REFERENCE, rt.applyTripUpdate(update));
  }

  @Test
  void invalidStopSequence() {
    var env = builder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);
    var update = rt.tripUpdateScheduled(TRIP_1_ID).addDelayedStopTime(100, 60).build();
    assertFailure(INVALID_STOP_SEQUENCE, rt.applyTripUpdate(update));
  }

  @Test
  void validAndInvalidStopSequence() {
    var env = builder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);
    var update = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addDelayedStopTime(0, 60)
      .addDelayedStopTime(100, 60)
      .build();
    assertFailure(INVALID_STOP_SEQUENCE, rt.applyTripUpdate(update));
  }

  /**
   * No stop id or stop sequence leads to a graceful failure.
   */
  @Test
  void noStopRef() {
    var env = builder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);
    var update = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addRawStopTime(
        StopTimeUpdate.newBuilder().setDeparture(StopTimeEvent.newBuilder().setDelay(60)).build()
      )
      .build();
    assertFailure(INVALID_STOP_REFERENCE, rt.applyTripUpdate(update));
  }
}
