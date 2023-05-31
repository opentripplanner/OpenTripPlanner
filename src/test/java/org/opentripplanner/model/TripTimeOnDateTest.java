package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TransitModelForTest.stopTime;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.TripTimes;

class TripTimeOnDateTest {

  static final int SEQUENCE = 99;

  @Test
  void gtfsSequence(){
    var pattern = TransitModelForTest.pattern(TransitMode.BUS).build();
    var trip = TransitModelForTest.trip("123").build();
    var sixOclock = (int) Duration.ofHours(18).toSeconds();
    var fivePast6 = sixOclock + 300;
    var tenPast6 = fivePast6 + 300;
    var stopTimes = List.of(stopTime(trip, 0, sixOclock), stopTime(trip, 5, fivePast6), stopTime(trip, SEQUENCE, tenPast6));

    var tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

    var subject = new TripTimeOnDate(tripTimes, 2, pattern);

    var seq = subject.getGtfsSequence();
    assertEquals(SEQUENCE, seq);

    var departure = LocalTime.ofSecondOfDay(subject.getScheduledDeparture());

    assertEquals(LocalTime.of(18, 10), departure);
  }


}