package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

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
}
