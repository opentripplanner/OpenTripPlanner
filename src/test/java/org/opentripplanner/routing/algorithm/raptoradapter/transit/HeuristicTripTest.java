package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_A;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_B;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_C;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_D;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_E;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestRoute;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

class HeuristicTripTest {

  /**
   * Test with five minutes of running time and no dwell time
   */
  @Test
  public void testSameArrivalDepartureTimes() {
    int[] times = { 0, 5, 10, 15, 20 };

    TestRoute route = TestRoute
      .route("R1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E)
      .withTimetable(
        schedule("00:10 00:15 00:20 00:25 00:30"),
        schedule("00:15 00:20 00:25 00:30 00:35"),
        schedule("00:20 00:25 00:30 00:35 00:40"),
        schedule("00:25 00:30 00:35 00:40 00:45")
      );

    RaptorTripSchedule subject = route.getHeuristicTrip();

    for (int i = 0; i < subject.pattern().numberOfStopsInPattern(); i++) {
      assertEquals(times[i], subject.departure(i) / 60);
      assertEquals(subject.arrival(i), subject.departure(i));
    }
  }

  /**
   * Test with four minutes of running time and one minute of dwell
   */
  @Test
  public void testDwellTimes() {
    int[] times = { 0, 4, 9, 14, 19 };
    int departureOffset = 60;

    TestRoute route = TestRoute
      .route("R1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E)
      .withTimetable(
        schedule().arrivals("00:10 00:15 00:20 00:25 00:30").arrDepOffset(departureOffset),
        schedule().arrivals("00:15 00:20 00:25 00:30 00:35").arrDepOffset(departureOffset),
        schedule().arrivals("00:20 00:25 00:30 00:35 00:40").arrDepOffset(departureOffset),
        schedule().arrivals("00:25 00:30 00:35 00:40 00:45").arrDepOffset(departureOffset)
      );

    RaptorTripSchedule subject = route.getHeuristicTrip();

    assertEquals(0, subject.arrival(0));
    assertEquals(0, subject.departure(0));

    for (int i = 1; i < subject.pattern().numberOfStopsInPattern(); i++) {
      assertEquals(times[i], subject.arrival(i) / 60);
      assertEquals(subject.arrival(i) + departureOffset, subject.departure(i));
    }
  }

  /**
   * Test with two trips, one with five minutes of running time and no dwell time and the other with
   * four minutes of running time and one minute of dwell
   */
  @Test
  public void testStaggeredTimes() {
    int[] times = { 0, 4, 9, 14, 19 };
    int departureOffset = 60;

    TestRoute route = TestRoute
      .route("R1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E)
      .withTimetable(
        schedule().arrivals("00:10 00:15 00:20 00:25 00:30").arrDepOffset(0),
        schedule().arrivals("10:10 10:15 10:20 10:25 10:30").arrDepOffset(departureOffset)
      );

    RaptorTripSchedule subject = route.getHeuristicTrip();

    assertEquals(0, subject.arrival(0));
    assertEquals(0, subject.departure(0));

    for (int i = 1; i < subject.pattern().numberOfStopsInPattern(); i++) {
      assertEquals(times[i], subject.arrival(i) / 60);
      assertEquals(subject.arrival(i) + departureOffset, subject.departure(i));
    }
  }

  /**
   * Test with two trips, one with five minutes of running time on the two first hosp and 10 minutes
   * on the last hops and a second trip with the hop durations reversed. Note that the last times
   * are less than either of the running times.
   */
  @Test
  public void testDifferentTimes() {
    int[] times = { 0, 5, 10, 15, 20 };

    TestRoute route = TestRoute
      .route("R1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E)
      .withTimetable(
        schedule().times("00:10 00:15 00:20 00:30 00:40"),
        schedule().times("10:10 10:20 10:30 10:35 10:40")
      );

    RaptorTripSchedule subject = route.getHeuristicTrip();

    assertEquals(0, subject.arrival(0));
    assertEquals(0, subject.departure(0));

    for (int i = 1; i < subject.pattern().numberOfStopsInPattern(); i++) {
      assertEquals(times[i], subject.arrival(i) / 60);
      assertEquals(subject.arrival(i), subject.departure(i));
    }
  }
}
