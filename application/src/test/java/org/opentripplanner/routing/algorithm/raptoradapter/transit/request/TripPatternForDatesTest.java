package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.BitSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class TripPatternForDatesTest {

  private static final int FREQUENCY_START = 7 * 60 * 60;
  private static final int FREQUENCY_END = 23 * 60 * 60;
  private static final int HEADWAY = 300;
  private static final Route ROUTE = TimetableRepositoryForTest.route("1").build();
  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 11, 1);

  @Test
  void forwardSearchInRange() {
    var result = getTestSubjectWithExactFrequency()
      .createCustomizedTripSearch(SearchDirection.FORWARD)
      .search(FREQUENCY_END - HEADWAY, 0);
    assertTrue(result.time() >= FREQUENCY_END - HEADWAY);
    assertTrue(result.time() < FREQUENCY_END);
  }

  @Test
  void forwardSearchOutOfRange() {
    var result = getTestSubjectWithExactFrequency()
      .createCustomizedTripSearch(SearchDirection.FORWARD)
      .search(FREQUENCY_END, 0);
    assertTrue(result.empty());
  }

  @Test
  void reverseSearchInRange() {
    var result = getTestSubjectWithExactFrequency()
      .createCustomizedTripSearch(SearchDirection.REVERSE)
      .search(FREQUENCY_START, 0);
    assertEquals(FREQUENCY_START, result.time());
  }

  @Test
  void reverseSearchOutOfRange() {
    var result = getTestSubjectWithExactFrequency()
      .createCustomizedTripSearch(SearchDirection.REVERSE)
      .search(FREQUENCY_START - 1, 0);
    assertTrue(result.empty());
  }

  private static TripPatternForDates getTestSubjectWithExactFrequency() {
    var testModel = TimetableRepositoryForTest.of();
    var stop1 = testModel.stop("FEED:STOP1", 0, 0).build();
    var stop2 = testModel.stop("FEED:STOP2", 0, 0).build();

    var stopTime1 = new StopTime();
    stopTime1.setStop(stop1);
    stopTime1.setArrivalTime(0);
    stopTime1.setDepartureTime(0);
    stopTime1.setStopSequence(0);
    var stopTime2 = new StopTime();
    stopTime2.setStop(stop2);
    stopTime2.setArrivalTime(300);
    stopTime2.setDepartureTime(300);
    stopTime2.setStopSequence(1);
    StopPattern stopPattern = new StopPattern(List.of(stopTime1, stopTime2));
    RoutingTripPattern tripPattern = TripPattern.of(TimetableRepositoryForTest.id("P1"))
      .withRoute(ROUTE)
      .withStopPattern(stopPattern)
      .build()
      .getRoutingTripPattern();

    final ScheduledTripTimes tripTimes = TripTimesFactory.tripTimes(
      TimetableRepositoryForTest.trip("1").withRoute(ROUTE).build(),
      List.of(stopTime1, stopTime2),
      new Deduplicator()
    );

    var frequency = new Frequency();
    frequency.setStartTime(FREQUENCY_START);
    frequency.setEndTime(FREQUENCY_END);
    frequency.setHeadwaySecs(HEADWAY);
    frequency.setExactTimes(1);

    var boardingAndAlightingPossible = new BitSet(2);
    boardingAndAlightingPossible.set(0);
    boardingAndAlightingPossible.set(1);

    return new TripPatternForDates(
      tripPattern,
      new TripPatternForDate[] {
        new TripPatternForDate(
          tripPattern,
          List.of(tripTimes),
          List.of(new FrequencyEntry(frequency, tripTimes)),
          SERVICE_DATE
        ),
      },
      new int[] { 0 },
      boardingAndAlightingPossible,
      boardingAndAlightingPossible,
      0
    );
  }
}
