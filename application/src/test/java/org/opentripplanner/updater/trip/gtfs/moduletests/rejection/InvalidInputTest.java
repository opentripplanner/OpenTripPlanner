package org.opentripplanner.updater.trip.gtfs.moduletests.rejection;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * A trip with start date that is outside the service period shouldn't throw an exception and is
 * ignored instead.
 */
class InvalidInputTest implements RealtimeTestConstants {

  public static List<LocalDate> cases() {
    return List.of(SERVICE_DATE.minusYears(10), SERVICE_DATE.plusYears(10));
  }

  @ParameterizedTest
  @MethodSource("cases")
  void invalidTripDate(LocalDate date) {
    var tripInput = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A1, "0:00:10", "0:00:11")
      .addStop(STOP_B1, "0:00:20", "0:00:21")
      .build();
    var env = RealtimeTestEnvironment.of().addTrip(tripInput).build();

    var update = new TripUpdateBuilder(TRIP_1_ID, date, SCHEDULED, TIME_ZONE)
      .addDelayedStopTime(2, 60, 80)
      .build();

    var result = env.applyTripUpdate(update);

    var snapshot = env.getTimetableSnapshot();
    assertTrue(snapshot.isEmpty());
    assertFailure(NO_SERVICE_ON_DATE, result);
  }
}
