package org.opentripplanner.transit.model.timetable;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * Tests {@link TripTimes#scheduledRunningTime(Instant)}.
 */
class TripTimesScheduledRunningTimeTest {

  private static final Trip TRIP = TransitRepositoryForTest.trip("Trip-1").build();
  private static final ZoneId OSLO = ZoneId.of("Europe/Oslo");

  @Test
  void runningTimeIsFromFirstDepartureToLastArrival() {
    var subject = tripTimes(
      new int[] { TimeUtils.time("10:00"), TimeUtils.time("11:25") },
      new int[] { TimeUtils.time("10:01"), TimeUtils.time("11:30") }
    );

    var period = subject.scheduledRunningTime(startOfService(LocalDate.of(2024, 6, 5)));

    assertThat(period).isNotNull();
    assertThat(period.start()).hasValue(instant("2024-06-05T08:01:00Z"));
    assertThat(period.end()).hasValue(instant("2024-06-05T09:25:00Z"));
  }

  @Test
  void runningTimeAfterMidnightIsOnTheNextDay() {
    var subject = tripTimes(
      new int[] { TimeUtils.time("23:50"), TimeUtils.time("24:30") },
      new int[] { TimeUtils.time("23:50"), TimeUtils.time("24:30") }
    );

    var period = subject.scheduledRunningTime(startOfService(LocalDate.of(2024, 6, 5)));

    assertThat(period).isNotNull();
    assertThat(period.start()).hasValue(instant("2024-06-05T21:50:00Z"));
    assertThat(period.end()).hasValue(instant("2024-06-05T22:30:00Z"));
  }

  @Test
  void runningTimeOnDaylightSavingTimeChangeUsesTheStartOfTheServiceDay() {
    // On 2024-03-31 the clock in Oslo is moved forward one hour at 02:00, hence the service day
    // starts at 23:00 local time on the previous day.
    var subject = tripTimes(
      new int[] { TimeUtils.time("01:30"), TimeUtils.time("10:00") },
      new int[] { TimeUtils.time("01:30"), TimeUtils.time("10:00") }
    );

    var period = subject.scheduledRunningTime(startOfService(LocalDate.of(2024, 3, 31)));

    assertThat(period).isNotNull();
    assertThat(period.start()).hasValue(instant("2024-03-30T23:30:00Z"));
    assertThat(period.end()).hasValue(instant("2024-03-31T08:00:00Z"));
  }

  @Test
  void missingArrivalTimeAtLastStopIsNotResolved() {
    var subject = tripTimes(
      new int[] { TimeUtils.time("10:00"), StopTime.MISSING_VALUE },
      new int[] { TimeUtils.time("10:00"), TimeUtils.time("11:00") }
    );

    assertThat(subject.getScheduledArrivalTime(1)).isEqualTo(StopTime.MISSING_VALUE);
    assertThat(subject.scheduledRunningTime(startOfService(LocalDate.of(2024, 6, 5)))).isNull();
  }

  @Test
  void arrivalBeforeDepartureIsNotResolved() {
    var subject = tripTimes(
      new int[] { TimeUtils.time("10:00"), TimeUtils.time("09:00") },
      new int[] { TimeUtils.time("10:00"), TimeUtils.time("09:00") }
    );

    assertThat(subject.scheduledRunningTime(startOfService(LocalDate.of(2024, 6, 5)))).isNull();
  }

  private static Instant startOfService(LocalDate serviceDate) {
    return ServiceDateUtils.asStartOfService(serviceDate, OSLO).toInstant();
  }

  private static Instant instant(String text) {
    return Instant.parse(text);
  }

  /**
   * Note: the arrival and departure times of the first and the last stop are the only ones used by
   * the running time, the times in between are irrelevant for these tests.
   */
  private static TripTimes tripTimes(int[] arrivalTimes, int[] departureTimes) {
    return ScheduledTripTimes.of()
      .withArrivalTimes(arrivalTimes)
      .withDepartureTimes(departureTimes)
      .withTrip(TRIP)
      .build();
  }
}
