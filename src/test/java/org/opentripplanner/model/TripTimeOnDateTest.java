package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.TripTimes;

class TripTimeOnDateTest implements PlanTestConstants {

  @Test
  void gtfsSequence() {
    var pattern = TransitModelForTest.pattern(TransitMode.BUS).build();
    var trip = TransitModelForTest.trip("123").build();
    var stopTimes = TransitModelForTest.stopTimesEvery5Minutes(3, trip, T11_00);

    var tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

    var subject = new TripTimeOnDate(tripTimes, 2, pattern);

    var seq = subject.getGtfsSequence();
    assertEquals(30, seq);

    var departure = LocalTime.ofSecondOfDay(subject.getScheduledDeparture());
    assertEquals(LocalTime.of(11, 10), departure);
  }
}
