package org.opentripplanner.updater.trip.gtfs.moduletests.addition;

import static com.google.common.truth.Truth.assertThat;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.DUPLICATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateErrorType.NOT_IMPLEMENTED_DIFFERENTIAL_DUPLICATED;
import static org.opentripplanner.updater.spi.UpdateErrorType.OUTSIDE_SERVICE_PERIOD;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.gtfs.GtfsRtTestHelper;

class DuplicatedTest implements RealtimeTestConstants {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 6, 22);
  private static final LocalTime TIME = LocalTime.of(13, 30);
  private static final String DUPLICATED_ID =
    TRIP_1_ID + ":duplicated:" + SERVICE_DATE + "T" + TIME;
  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop stopA = envBuilder.stop(STOP_A_ID);
  private final RegularStop stopB = envBuilder.stop(STOP_B_ID);
  private final RegularStop stopC = envBuilder.stop(STOP_C_ID);

  private final TransitTestEnvironment env = envBuilder
    .addStops(STOP_A_ID, STOP_B_ID, STOP_C_ID)
    .addTrip(
      TripInput.of(TRIP_1_ID)
        .withServiceDates(SERVICE_DATE, SERVICE_DATE.plusDays(2))
        .addStop(stopA, "12:00")
        .addStop(stopB, "12:10")
        .addStop(stopC, "12:20")
    )
    .build();
  private final GtfsRtTestHelper gtfsRt = GtfsRtTestHelper.of(env);

  @Test
  void duplicated() {
    var tripUpdate = gtfsRt
      .tripUpdate(TRIP_1_ID, DUPLICATED)
      .withStartDate(SERVICE_DATE)
      .withStartTime(TIME)
      .build();

    assertThat(env.raptorData(SERVICE_DATE).summarizePatterns()).containsExactly("F:Pattern1[S]");
    assertSuccess(gtfsRt.applyTripUpdate(tripUpdate));

    assertEquals(
      "A U | A 13:30 13:30 | B 13:40 13:40 | C 13:50 13:50",
      env.tripData(DUPLICATED_ID, SERVICE_DATE).showTimetable()
    );

    assertThat(env.raptorData(SERVICE_DATE).summarizePatterns()).containsExactly(
      "F:Pattern1[S,A U]"
    );
  }

  @Test
  @Disabled(
    "Adding a trip on a service date where the original pattern is not running is not supported by the RAPTOR transit data"
  )
  void duplicatedOnDifferentServiceDate() {
    var date = SERVICE_DATE.plusDays(1);
    var tripUpdate = gtfsRt
      .tripUpdate(TRIP_1_ID, DUPLICATED)
      .withStartDate(date)
      .withStartTime(TIME)
      .build();

    assertThat(env.raptorData(date).summarizePatterns()).isEmpty();
    assertSuccess(gtfsRt.applyTripUpdate(tripUpdate));

    assertEquals(
      "A U | A 13:30 13:30 | B 13:40 13:40 | C 13:50 13:50",
      env.tripData(DUPLICATED_ID, date).showTimetable()
    );

    assertThat(env.raptorData(date).summarizePatterns()).containsExactly("F:Pattern1[A U]");
  }

  @Test
  void invalidIncrementality() {
    var tripUpdate = gtfsRt
      .tripUpdate(TRIP_1_ID, DUPLICATED)
      .withStartDate(SERVICE_DATE)
      .withStartTime(LocalTime.of(13, 0))
      .build();

    assertFailure(
      NOT_IMPLEMENTED_DIFFERENTIAL_DUPLICATED,
      gtfsRt.applyTripUpdate(tripUpdate, DIFFERENTIAL)
    );
  }

  @Test
  void originalTripNotFound() {
    var tripUpdate = gtfsRt
      .tripUpdate("not-found", DUPLICATED)
      .withStartDate(SERVICE_DATE)
      .withStartTime(LocalTime.of(13, 0))
      .build();

    assertFailure(TRIP_NOT_FOUND, gtfsRt.applyTripUpdate(tripUpdate));
  }

  @Test
  void outsideServicePeriod() {
    var tripUpdate = gtfsRt
      .tripUpdate(TRIP_1_ID, DUPLICATED)
      .withStartDate(SERVICE_DATE.plusYears(1))
      .withStartTime(LocalTime.of(13, 0))
      .build();

    assertFailure(OUTSIDE_SERVICE_PERIOD, gtfsRt.applyTripUpdate(tripUpdate));
  }
}
