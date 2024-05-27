package org.opentripplanner.updater.trip.moduletests.rejection;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_SERVICE_ON_DATE;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.updater.trip.GtfsRealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * A trip with start date that is outside the service period shouldn't throw an exception and is
 * ignored instead.
 */
public class InvalidInputTest {

  @Test
  public void invalidTripDate() {
    var env = new GtfsRealtimeTestEnvironment();

    var serviceDateOutsideService = env.serviceDate.minusYears(10);
    var builder = new TripUpdateBuilder(
      env.trip1.getId().getId(),
      serviceDateOutsideService,
      SCHEDULED,
      env.timeZone
    )
      .addDelayedStopTime(1, 0)
      .addDelayedStopTime(2, 60, 80)
      .addDelayedStopTime(3, 90, 90);

    var tripUpdate = builder.build();

    var result = env.applyTripUpdates(tripUpdate);

    final TimetableSnapshot snapshot = env.source.getTimetableSnapshot();
    assertNull(snapshot);
    assertEquals(1, result.failed());
    var errors = result.failures();
    assertEquals(1, errors.get(NO_SERVICE_ON_DATE).size());
  }
}
