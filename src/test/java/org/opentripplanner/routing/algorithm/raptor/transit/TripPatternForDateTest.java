package org.opentripplanner.routing.algorithm.raptor.transit;

import org.junit.Test;
import org.mockito.Mockito;
import org.opentripplanner.model.*;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TripPatternForDateTest {

  private static final FeedScopedId TEST_ROUTE_ID = new FeedScopedId("TEST", "ROUTE");

  private static final Stop STOP_FOR_TEST = Stop.stopForTest("TEST:STOP", 0, 0);

  private static final TripTimes[] TEST_TRIP_TIMES = new TripTimes[] {
      Mockito.mock(TripTimes.class), Mockito.mock(TripTimes.class)
  };

  @Test
  public void returnSameIfNotFilteredTest() {
    TripPatternForDate testTripPatternForDate = createTestTripPatternForDate();

    TripPatternForDate filteredTripTimes = testTripPatternForDate.newWithFilteredTripTimes(
        (tripTimes) -> true
    );

    assertEquals(testTripPatternForDate, filteredTripTimes);
  }


  private TripPatternForDate createTestTripPatternForDate() {
    Route route = new Route(TEST_ROUTE_ID);
    route.setMode(TransitMode.BUS);

    var stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    StopPattern stopPattern = new StopPattern(List.of(stopTime));
    TripPattern pattern = new TripPattern(null, route, stopPattern);

    TripPatternWithRaptorStopIndexes tripPattern = new TripPatternWithRaptorStopIndexes(
        new int[0], pattern
    );

    return new TripPatternForDate(tripPattern, TEST_TRIP_TIMES, LocalDate.now());
  }

  @Test
  public void returnNewIfFilteredTest() {
    TripPatternForDate testTripPatternForDate = createTestTripPatternForDate();

    TripPatternForDate filteredTripTimes = testTripPatternForDate.newWithFilteredTripTimes(
        (tripTimes) -> tripTimes.equals(TEST_TRIP_TIMES[0])
    );

    assertNotEquals(testTripPatternForDate, filteredTripTimes);
    assertEquals(1, filteredTripTimes.numberOfTripSchedules());
  }

}
