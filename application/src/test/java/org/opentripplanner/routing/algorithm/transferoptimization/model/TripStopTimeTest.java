package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.StopTime.stopTime;
import static org.opentripplanner.utils.time.TimeUtils.time;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptorlegacy._data.transit.TestTripPattern;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.utils.time.TimeUtils;

public class TripStopTimeTest {

  private static final int STOP_1 = 2;
  private static final int STOP_2 = 5;
  private static final int STOP_3 = 7;

  TestTripSchedule trip = TestTripSchedule.schedule(
    TestTripPattern.pattern("L31", STOP_1, STOP_2, STOP_3)
  )
    .arrivals("10:00 10:05 10:20")
    .departures("10:01 10:06 10:21")
    .build();

  private final TripStopTime<TestTripSchedule> arrivalStop2 = TripStopTime.arrival(trip, 1);
  private final TripStopTime<TestTripSchedule> arrivalStop3 = TripStopTime.arrival(trip, 2);

  private final TripStopTime<TestTripSchedule> departureStop1 = TripStopTime.departure(trip, 0);
  private final TripStopTime<TestTripSchedule> departureStop2 = TripStopTime.departure(trip, 1);

  @Test
  public void stopPosition() {
    assertEquals(0, departureStop1.stopPosition());
    assertEquals(1, arrivalStop2.stopPosition());
    assertEquals(1, departureStop2.stopPosition());
    assertEquals(2, arrivalStop3.stopPosition());
  }

  @Test
  public void stop() {
    assertEquals(STOP_1, departureStop1.stop());
    assertEquals(STOP_2, arrivalStop2.stop());
    assertEquals(STOP_2, departureStop2.stop());
    assertEquals(STOP_3, arrivalStop3.stop());
  }

  @Test
  public void testTime() {
    assertEquals("10:01:00", TimeUtils.timeToStrLong(departureStop1.time()));
    assertEquals("10:20:00", TimeUtils.timeToStrLong(arrivalStop3.time()));
  }

  @Test
  public void trip() {
    String expected =
      "TestTripSchedule{arrivals: [10:00 10:05 10:20], departures: [10:01 10:06 10:21]}";
    assertEquals(expected, departureStop1.trip().toString());
    assertEquals(expected, arrivalStop3.trip().toString());
  }

  @Test
  public void testToString() {
    assertEquals("[2 10:01 BUS L31]", departureStop1.toString());
    assertEquals("[7 10:20 BUS L31]", arrivalStop3.toString());
  }

  @Test
  public void createArrival() {
    assertEquals(
      "[2 10:00 BUS L31]",
      TripStopTime.arrival(trip, stopTime(STOP_1, time("10:00"))).toString()
    );
    assertEquals(
      "[5 10:05 BUS L31]",
      TripStopTime.arrival(trip, stopTime(STOP_2, time("10:05"))).toString()
    );
    assertEquals(
      "[7 10:20 BUS L31]",
      TripStopTime.arrival(trip, stopTime(STOP_3, time("10:20"))).toString()
    );
  }

  @Test
  public void createDeparture() {
    assertEquals(
      "[2 10:01 BUS L31]",
      TripStopTime.departure(trip, stopTime(STOP_1, time("10:01"))).toString()
    );
    assertEquals(
      "[5 10:06 BUS L31]",
      TripStopTime.departure(trip, stopTime(STOP_2, time("10:06"))).toString()
    );
    assertEquals(
      "[7 10:21 BUS L31]",
      TripStopTime.departure(trip, stopTime(STOP_3, time("10:21"))).toString()
    );
  }

  @Test
  public void testEquals() {
    var same = TripStopTime.departure(trip, 0);
    assertEquals(departureStop1, same);
    assertEquals(departureStop1.hashCode(), same.hashCode());
  }
}
