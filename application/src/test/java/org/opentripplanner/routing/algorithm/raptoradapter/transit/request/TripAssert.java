package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;

class TripAssert {

  private final RaptorTripScheduleSearch<TestTripSchedule> subject;
  private RaptorBoardOrAlightEvent<TestTripSchedule> result;
  private int stopPosition;

  TripAssert(RaptorTripScheduleSearch<TestTripSchedule> subject) {
    this.subject = subject;
  }

  /**
   * @param arrivalTime  The search add the boardSlack, so we use 'arrivalTime' as input to the
   *                     search
   * @param stopPosition The stop position to board
   */
  TripAssert search(int arrivalTime, int stopPosition) {
    this.stopPosition = stopPosition;
    result = subject.search(arrivalTime, stopPosition);
    return this;
  }

  /**
   * @param arrivalTime    The search add the boardSlack, so we use 'arrivalTime' as input to the
   *                       search
   * @param stopPosition   The stop position to board
   * @param tripIndexLimit the index of a previous tripSchedule found, used to search for a better
   *                       trip. (exclusive)
   */
  TripAssert search(int arrivalTime, int stopPosition, int tripIndexLimit) {
    this.stopPosition = stopPosition;
    result = subject.search(arrivalTime, stopPosition, tripIndexLimit);
    return this;
  }

  void assertNoTripFound() {
    assertTrue(result.empty(), "No trip expected, but trip found with index: " + result);
  }

  TripAssert assertTripFound() {
    assertFalse(result.empty(), "Trip expected, but trip found");
    return this;
  }

  TripAssert withIndex(int expectedTripIndex) {
    assertEquals(expectedTripIndex, result.tripIndex(), "Trip index");
    return this;
  }

  TripAssert withBoardTime(int expectedBoardTime) {
    assertEquals(expectedBoardTime, result.trip().departure(stopPosition), "Board time");
    return this;
  }

  TripAssert withAlightTime(int expectedAlightTime) {
    assertEquals(expectedAlightTime, result.trip().arrival(stopPosition), "Alight time");
    return this;
  }
}
