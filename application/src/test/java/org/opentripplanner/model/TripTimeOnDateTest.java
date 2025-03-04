package org.opentripplanner.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class TripTimeOnDateTest implements PlanTestConstants {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

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

    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());
    tripTimes.setRecorded(1);

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

  private static TripTimeOnDate tripTimeOnDate() {
    var trip = TimetableRepositoryForTest.trip("123").build();
    var stopTimes = TEST_MODEL.stopTimesEvery5Minutes(5, trip, "11:00");
    var stops = stopTimes.stream().map(StopTime::getStop).map(RegularStop.class::cast).toList();
    var pattern = TEST_MODEL.pattern(TransitMode.BUS)
      .withStopPattern(TimetableRepositoryForTest.stopPattern(stops))
      .build();
    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());
    return new TripTimeOnDate(tripTimes, 2, pattern);
  }
}
