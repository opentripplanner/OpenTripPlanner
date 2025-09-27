package org.opentripplanner.updater.trip.gtfs.moduletests.addition;

import static com.google.common.truth.Truth.assertThat;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.NEW;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.OUTSIDE_SERVICE_PERIOD;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

class AddedOnServiceDateTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder envBuilder = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A = envBuilder.stop(STOP_A_ID);
  private final RegularStop STOP_B = envBuilder.stop(STOP_B_ID);
  private final RegularStop STOP_C = envBuilder.stop(STOP_C_ID);

  private static final LocalDate START_DATE = SERVICE_DATE;
  private static final LocalDate ADDED_DATE = START_DATE.plusDays(1);
  private static final LocalDate END_DATE = START_DATE.plusDays(2);

  private final RealtimeTestEnvironment env = envBuilder
    .addTrip(
      TripInput.of(TRIP_1_ID)
        // on either side of added date, but not on it
        .withServiceDates(START_DATE, END_DATE)
        .addStop(STOP_A, "12:00")
        .addStop(STOP_B, "12:10")
        .addStop(STOP_C, "12:20")
        .build()
    )
    .build();

  private static List<LocalDate> serviceDates() {
    return List.of(START_DATE, ADDED_DATE, END_DATE);
  }

  @ParameterizedTest
  @MethodSource("serviceDates")
  void addedTrip(LocalDate date) {
    var tripUpdate = new TripUpdateBuilder(ADDED_TRIP_ID, date, NEW, TIME_ZONE)
      .addStopTime(STOP_A_ID, "10:30")
      .addStopTime(STOP_B_ID, "10:40")
      .addStopTime(STOP_C_ID, "10:55")
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate));
    assertNotNull(env.getPatternForTrip(id(ADDED_TRIP_ID), date));

    var trip = env.getTripTimesForTrip(id(ADDED_TRIP_ID), date).getTrip();
    var dates = env
      .getTransitService()
      .getCalendarService()
      .getServiceDatesForServiceId(trip.getServiceId());
    assertThat(dates).containsExactly(date);
  }

  private static List<LocalDate> outsidePeriod() {
    return List.of(START_DATE.minusYears(1), END_DATE.plusYears(1));
  }

  @ParameterizedTest
  @MethodSource("outsidePeriod")
  void rejectOutsideSchedulePeriod(LocalDate date) {
    var tripUpdate = new TripUpdateBuilder(ADDED_TRIP_ID, date, NEW, TIME_ZONE)
      .addStopTime(STOP_A_ID, "10:30")
      .addStopTime(STOP_B_ID, "10:40")
      .addStopTime(STOP_C_ID, "10:55")
      .build();

    assertFailure(OUTSIDE_SERVICE_PERIOD, env.applyTripUpdate(tripUpdate));
  }
}
