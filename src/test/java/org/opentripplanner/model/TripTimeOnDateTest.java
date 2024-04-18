package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class TripTimeOnDateTest implements PlanTestConstants {

  @Test
  void gtfsSequence() {
    final var subject = buildTripTimeOnDate(2);

    var seq = subject.getGtfsSequence();
    assertEquals(30, seq);

    var departure = LocalTime.ofSecondOfDay(subject.getScheduledDeparture());
    assertEquals(LocalTime.of(11, 10), departure);
  }

  @Test
  void tripPosition() {
    assertEquals(StopPositionType.FIRST, buildTripTimeOnDate(0).stopPositionType());
    assertEquals(StopPositionType.MIDDLE, buildTripTimeOnDate(1).stopPositionType());
    assertEquals(StopPositionType.LAST, buildTripTimeOnDate(2).stopPositionType());
  }

  private static TripTimeOnDate buildTripTimeOnDate(int stopIndex) {
    var testModel = TransitModelForTest.of();
    var pattern = testModel.pattern(TransitMode.BUS).build();
    var trip = TransitModelForTest.trip("123").build();
    var stopTimes = testModel.stopTimesEvery5Minutes(3, trip, T11_00);

    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

    return new TripTimeOnDate(tripTimes, stopIndex, pattern);
  }
}
