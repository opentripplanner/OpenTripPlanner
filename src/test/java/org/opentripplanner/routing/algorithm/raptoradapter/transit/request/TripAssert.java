package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;

class TripAssert {

  private final RaptorTripScheduleSearch<TestTripSchedule> subject;
  private RaptorTripScheduleBoardOrAlightEvent<TestTripSchedule> result;
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
    assertNull(result, "No trip expected, but trip found with index: " + result);
  }

  TripAssert assertTripFound() {
    assertNotNull(result, "Trip expected, but trip found");
    return this;
  }

  TripAssert withIndex(int expectedTripIndex) {
    assertEquals(expectedTripIndex, result.getTripIndex(), "Trip index");
    return this;
  }

  TripAssert withBoardTime(int expectedBoardTime) {
    assertEquals(expectedBoardTime, result.getTrip().departure(stopPosition), "Board time");
    return this;
  }

  TripAssert withAlightTime(int expectedAlightTime) {
    assertEquals(expectedAlightTime, result.getTrip().arrival(stopPosition), "Alight time");
    return this;
  }
}
