package org.opentripplanner.updater.trip.moduletests.rejection;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.updater.trip.RealtimeTestEnvironment.SERVICE_DATE;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * A trip with start date that is outside the service period shouldn't throw an exception and is
 * ignored instead.
 */
public class InvalidInputTest {

  public static List<LocalDate> cases() {
    return List.of(SERVICE_DATE.minusYears(10), SERVICE_DATE.plusYears(10));
  }

  @ParameterizedTest
  @MethodSource("cases")
  public void invalidTripDate(LocalDate date) {
    var env = RealtimeTestEnvironment.gtfs();

    var update = new TripUpdateBuilder(env.trip1.getId().getId(), date, SCHEDULED, env.timeZone)
      .addDelayedStopTime(1, 0)
      .addDelayedStopTime(2, 60, 80)
      .addDelayedStopTime(3, 90, 90)
      .build();

    var result = env.applyTripUpdate(update);

    var snapshot = env.getTimetableSnapshot();
    assertTrue(snapshot.isEmpty());
    assertEquals(1, result.failed());
    var errors = result.failures().keySet();
    assertEquals(Set.of(NO_SERVICE_ON_DATE), errors);
  }
}
