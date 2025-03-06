package org.opentripplanner.raptor.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.utils.time.TimeUtils.timeToStrLong;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestTripPattern;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;

public class RaptorTripScheduleTest {

  private final TestTripSchedule subject = TestTripSchedule.schedule(
    TestTripPattern.pattern("L23", 1, 1, 2, 3, 5, 8, 1)
  )
    .arrivals("10:00 10:05 10:15 10:25 10:35 10:45 10:55")
    .departures("10:01 10:06 10:16 10:26 10:36 10:46 10:56")
    .build();

  @Test
  public void arrival() {
    assertEquals("10:00:00", timeToStrLong(subject.arrival(0)));
    assertEquals("10:05:00", timeToStrLong(subject.arrival(1)));
    assertEquals("10:55:00", timeToStrLong(subject.arrival(6)));
  }

  @Test
  public void departure() {
    assertEquals("10:01:00", timeToStrLong(subject.departure(0)));
    assertEquals("10:46:00", timeToStrLong(subject.departure(5)));
    assertEquals("10:56:00", timeToStrLong(subject.departure(6)));
  }

  @Test
  public void findArrivalStopPosition() {
    assertEquals("10:00:00", timeToStrLong(subject.arrival(0, 1)));
    assertEquals("10:05:00", timeToStrLong(subject.arrival(1, 1)));
    assertEquals("10:55:00", timeToStrLong(subject.arrival(2, 1)));
  }

  @Test
  public void findDepartureStopPosition() {
    assertEquals("10:01:00", timeToStrLong(subject.departure(0, 1)));
    assertEquals("10:46:00", timeToStrLong(subject.departure(0, 8)));
    assertEquals("10:56:00", timeToStrLong(subject.departure(2, 1)));
  }
}
