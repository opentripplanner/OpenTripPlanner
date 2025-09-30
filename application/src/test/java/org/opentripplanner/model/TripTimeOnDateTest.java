package org.opentripplanner.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.utils.time.ServiceDateUtils;

class TripTimeOnDateTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final LocalDate DATE = LocalDate.of(2025, 3, 18);
  private static final Instant MIDNIGHT = ServiceDateUtils.asStartOfService(
    DATE,
    ZoneIds.BERLIN
  ).toInstant();

  @Test
  void gtfsSequence() {
    var pattern = TEST_MODEL.pattern(TransitMode.BUS).build();
    var trip = TimetableRepositoryForTest.trip("123").build();
    var stopTimes = TEST_MODEL.stopTimesEvery5Minutes(3, trip, "11:00");

    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

    var subject = new TripTimeOnDate(tripTimes, 2, pattern);

    var seq = subject.getGtfsSequence();
    assertEquals(30, seq);

    var departure = LocalTime.ofSecondOfDay(subject.getScheduledDeparture());
    assertEquals(LocalTime.of(11, 10), departure);
  }

  @Test
  void isRecordedStop() {
    var pattern = TEST_MODEL.pattern(TransitMode.BUS).build();
    var trip = TimetableRepositoryForTest.trip("123").build();
    var stopTimes = TEST_MODEL.stopTimesEvery5Minutes(3, trip, "11:00");

    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator())
      .createRealTimeFromScheduledTimes()
      .withRecorded(1)
      .build();

    var subject = new TripTimeOnDate(tripTimes, 0, pattern);

    assertFalse(subject.isRecordedStop());

    subject = new TripTimeOnDate(tripTimes, 1, pattern);

    assertTrue(subject.isRecordedStop());
  }

  @Test
  void previousTimes() {
    var subject = tripTimeOnDate();

    var ids = subject.previousTimes().stream().map(t -> t.getStop().getId().toString()).toList();
    assertEquals(List.of("F:stop-10", "F:stop-20"), ids);
    assertThat(subject.previousTimes().getFirst().previousTimes()).isEmpty();
  }

  @Test
  void nextTimes() {
    var subject = tripTimeOnDate();
    var ids = subject.nextTimes().stream().map(t -> t.getStop().getId().toString()).toList();
    assertEquals(List.of("F:stop-40", "F:stop-50"), ids);
    var secondLast = subject.nextTimes().getFirst();
    var lastStop = secondLast
      .nextTimes()
      .stream()
      .map(t -> t.getStop().getId().toString())
      .toList();
    assertEquals(List.of("F:stop-50"), lastStop);
    assertThat(secondLast.nextTimes().getFirst().nextTimes()).isEmpty();
  }

  @Test
  void atZone() {
    var subject = tripTimeOnDate();
    var departure = subject.scheduledDeparture();
    assertEquals(
      "2025-03-18T11:10+01:00",
      departure.atZone(ZoneIds.BERLIN).toOffsetDateTime().toString()
    );
  }

  private static TripTimeOnDate tripTimeOnDate() {
    var trip = TimetableRepositoryForTest.trip("123").build();
    var stopTimes = TEST_MODEL.stopTimesEvery5Minutes(5, trip, "11:00");
    var stops = stopTimes.stream().map(StopTime::getStop).toList();
    var pattern = TEST_MODEL.pattern(TransitMode.BUS)
      .withStopPattern(TimetableRepositoryForTest.stopPattern(stops))
      .build();
    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());
    return new TripTimeOnDate(tripTimes, 2, pattern, DATE, MIDNIGHT);
  }

  @Test
  void testFromTripTimesWithScheduleFallback() {
    var testModel = TimetableRepositoryForTest.of();
    var trip = TimetableRepositoryForTest.trip("123").build();
    var siteRepository = testModel.siteRepositoryBuilder().build();
    var timetableRepository = new TimetableRepository(siteRepository, new Deduplicator());
    var tripTimes = ScheduledTripTimes.of()
      .withTrip(trip)
      .withDepartureTimes(new int[] { 0, 1 })
      .build();
    var tripPattern = testModel
      .pattern(TransitMode.BUS)
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
      .build();
    timetableRepository.addTripPattern(tripPattern.getId(), tripPattern);
    timetableRepository.index();
    var timetableSnapshot = new TimetableSnapshot();
    timetableSnapshot.commit();
    var transitService = new DefaultTransitService(timetableRepository, timetableSnapshot);
    var serviceDate = LocalDate.of(2025, 1, 1);
    // Construct a timetable which definitely does not contain this trip, because it is empty.
    Timetable timetable = Timetable.of()
      .withTripPattern(tripPattern)
      .withServiceDate(serviceDate)
      .build();
    Instant midnight = ServiceDateUtils.asStartOfService(serviceDate, ZoneIds.HELSINKI).toInstant();
    var tripTimeOnDates = TripTimeOnDate.fromTripTimesWithScheduleFallback(
      timetable,
      trip,
      serviceDate,
      midnight
    );
    int i = 0;
    for (var tripTimeOnDate : tripTimeOnDates) {
      assertNull(tripTimeOnDate.getServiceDay());
      assertEquals(tripTimeOnDate.getServiceDayMidnight(), TripTimeOnDate.UNDEFINED);
      assertEquals(tripTimeOnDate.getTripTimes(), tripTimes);
      assertEquals(tripTimeOnDate.getStopPosition(), i);
      i++;
    }
  }
}
