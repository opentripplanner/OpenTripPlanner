package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.utils.time.ServiceDateUtils;

class TripTimeOnDateTest implements PlanTestConstants {

  @Test
  void gtfsSequence() {
    var testModel = TimetableRepositoryForTest.of();
    var pattern = testModel.pattern(TransitMode.BUS).build();
    var trip = TimetableRepositoryForTest.trip("123").build();
    var stopTimes = testModel.stopTimesEvery5Minutes(3, trip, "11:00");

    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

    var subject = new TripTimeOnDate(tripTimes, 2, pattern);

    var seq = subject.getGtfsSequence();
    assertEquals(30, seq);

    var departure = LocalTime.ofSecondOfDay(subject.getScheduledDeparture());
    assertEquals(LocalTime.of(11, 10), departure);
  }

  @Test
  void isRecordedStop() {
    var testModel = TimetableRepositoryForTest.of();
    var pattern = testModel.pattern(TransitMode.BUS).build();
    var trip = TimetableRepositoryForTest.trip("123").build();
    var stopTimes = testModel.stopTimesEvery5Minutes(3, trip, "11:00");

    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());
    tripTimes.setRecorded(1);

    var subject = new TripTimeOnDate(tripTimes, 0, pattern);

    assertFalse(subject.isRecordedStop());

    subject = new TripTimeOnDate(tripTimes, 1, pattern);

    assertTrue(subject.isRecordedStop());
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
      midnight,
      transitService
    );
    for (var tripTimeOnDate : tripTimeOnDates) {
      assertNull(tripTimeOnDate.getServiceDay());
      assertEquals(tripTimeOnDate.getServiceDayMidnight(), TripTimeOnDate.UNDEFINED);
    }
  }
}
