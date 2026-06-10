package org.opentripplanner.raptor.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.utils.time.TimeUtils.time;
import static org.opentripplanner.utils.time.TimeUtils.timeToStrLong;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestTripPattern;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;

class RaptorTripScheduleTest {

  private static final int STOP_A = 3;
  private static final int STOP_B = 7;
  private static final int STOP_C = 11;
  private static final int STOP_D = 13;
  private static final int STOP_E = 17;
  private static final int STOP_F = 21;

  private final TestTripSchedule subject = TestTripSchedule.schedule(
    TestTripPattern.pattern("L23", STOP_A, STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_A)
  )
    .arrivals("10:00 10:05 10:15 10:25 10:35 10:45 10:55")
    .departures("10:01 10:06 10:16 10:26 10:36 10:46 10:56")
    .build();

  @Test
  void arrival() {
    assertEquals("10:00:00", timeToStrLong(subject.arrival(0)));
    assertEquals("10:05:00", timeToStrLong(subject.arrival(1)));
    assertEquals("10:55:00", timeToStrLong(subject.arrival(6)));
  }

  @Test
  void departure() {
    assertEquals("10:01:00", timeToStrLong(subject.departure(0)));
    assertEquals("10:46:00", timeToStrLong(subject.departure(5)));
    assertEquals("10:56:00", timeToStrLong(subject.departure(6)));
  }

  @Test
  void findArrivalStopPosition() {
    assertEquals("10:00:00", timeToStrLong(subject.arrival(0, STOP_A)));
    assertEquals("10:05:00", timeToStrLong(subject.arrival(1, STOP_A)));
    assertEquals("10:55:00", timeToStrLong(subject.arrival(2, STOP_A)));
  }

  @Test
  void findDepartureStopPosition() {
    assertEquals("10:01:00", timeToStrLong(subject.departure(0, STOP_A)));
    assertEquals("10:46:00", timeToStrLong(subject.departure(0, STOP_E)));
    assertEquals("10:56:00", timeToStrLong(subject.departure(2, STOP_A)));
  }

  @Test
  void restrictedFindArrivalStopPosition() {
    var subject = TestTripSchedule.schedule(
      TestTripPattern.of("flex-with-repeating-stops", STOP_F, STOP_A, STOP_A, STOP_B, STOP_C)
        .restrictions("B * * - *")
        .build()
    )
      .times(
        time("08:50"),
        time("09:00"),
        time("09:10"),
        RaptorConstants.TIME_NOT_SET,
        time("09:30")
      )
      .build();

    assertEquals(1, subject.findArrivalStopPosition(time("09:06"), STOP_A));
  }

  @Test
  void restrictedFindDepartureStopPosition() {
    var subject = TestTripSchedule.schedule(
      TestTripPattern.of("restricted-repeating-stops", 1, 1, 1, 3).restrictions("* A * *").build()
    )
      .times(time("09:00"), time("09:10"), time("09:15"), time("09:30"))
      .build();

    assertEquals(2, subject.findDepartureStopPosition(time("09:09"), 1));
  }

  @Test
  void findStopPositionInCircularPattern() {
    var subject = TestTripSchedule.schedule(
      TestTripPattern.of("circular-loop", STOP_A, STOP_C, STOP_A).build()
    )
      .times(time("09:00"), time("09:10"), time("09:20"))
      .build();

    // find boarding
    assertEquals(0, subject.findDepartureStopPosition(time("09:00"), STOP_A));
    assertEquals(-1, subject.findDepartureStopPosition(time("09:01"), STOP_A));
    assertEquals(1, subject.findDepartureStopPosition(time("09:10"), STOP_C));
    assertEquals(-1, subject.findDepartureStopPosition(time("09:11"), STOP_C));

    // find alighting
    assertEquals(2, subject.findArrivalStopPosition(time("09:20"), STOP_A));
    assertEquals(-1, subject.findArrivalStopPosition(time("09:19"), STOP_A));
    assertEquals(1, subject.findArrivalStopPosition(time("09:10"), STOP_C));
    assertEquals(-1, subject.findArrivalStopPosition(time("09:09"), STOP_C));
  }
}
