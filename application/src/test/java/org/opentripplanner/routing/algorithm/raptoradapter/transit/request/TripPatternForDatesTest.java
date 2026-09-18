package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.BitSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.raptor.spi.SearchDirection;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class TripPatternForDatesTest {

  private static final int FREQUENCY_START = 7 * 60 * 60;
  private static final int FREQUENCY_END = 23 * 60 * 60;
  private static final int HEADWAY = 300;
  private static final Route ROUTE = TransitRepositoryForTest.route("1").build();
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

  @Test
  void regularTripTimesUseSourceValuesAndServiceDateOffset() {
    var tripPattern = createTripPattern();
    var firstTrip = createTripTimes("regular-1", new int[] { 0, 600 }, new int[] { 120, 720 });
    var secondTrip = createTripTimes("regular-2", new int[] { 60, 660 }, new int[] { 180, 780 });

    var subject = new TripPatternForDates(
      tripPattern,
      new TripPatternForDate[] {
        new TripPatternForDate(tripPattern, List.of(firstTrip), List.of(), SERVICE_DATE),
        new TripPatternForDate(
          tripPattern,
          List.of(secondTrip),
          List.of(),
          SERVICE_DATE.plusDays(1)
        ),
      },
      new int[] { 0, 24 * 60 * 60 },
      allStops(),
      allStops(),
      0
    );

    assertEquals(0, subject.arrivalTime(0, 0));
    assertEquals(120, subject.departureTime(0, 0));
    assertEquals(600, subject.arrivalTime(1, 0));
    assertEquals(720, subject.departureTime(1, 0));
    assertEquals(24 * 60 * 60 + 60, subject.arrivalTime(0, 1));
    assertEquals(24 * 60 * 60 + 180, subject.departureTime(0, 1));
    assertEquals(24 * 60 * 60 + 660, subject.arrivalTime(1, 1));
    assertEquals(24 * 60 * 60 + 780, subject.departureTime(1, 1));
  }

  @Test
  void regularTripTimesPreserveOvernightOrderAndNegativeOffset() {
    var tripPattern = createTripPattern();
    var overnightTrip = createTripTimes(
      "overnight",
      new int[] { 24 * 60 * 60, 24 * 60 * 60 + 600 },
      new int[] { 24 * 60 * 60 + 120, 24 * 60 * 60 + 720 }
    );
    var nextDayTrip = createTripTimes("next-day", new int[] { 10, 610 }, new int[] { 130, 730 });

    var subject = new TripPatternForDates(
      tripPattern,
      new TripPatternForDate[] {
        new TripPatternForDate(tripPattern, List.of(overnightTrip), List.of(), SERVICE_DATE),
        new TripPatternForDate(
          tripPattern,
          List.of(nextDayTrip),
          List.of(),
          SERVICE_DATE.plusDays(1)
        ),
      },
      new int[] { -24 * 60 * 60, 0 },
      allStops(),
      allStops(),
      0
    );

    assertEquals(0, subject.arrivalTime(0, 0));
    assertEquals(120, subject.departureTime(0, 0));
    assertEquals(10, subject.arrivalTime(0, 1));
    assertEquals(130, subject.departureTime(0, 1));
    assertEquals(610, subject.arrivalTime(1, 1));
    assertEquals(730, subject.departureTime(1, 1));
  }

  private static TripPatternForDates getTestSubjectWithExactFrequency() {
    var testModel = TransitRepositoryForTest.of();
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
    RoutingTripPattern tripPattern = TripPattern.of(FeedScopedIdForTestFactory.id("P1"))
      .withRoute(ROUTE)
      .withStopPattern(stopPattern)
      .build()
      .getRoutingTripPattern();

    Trip trip = TransitRepositoryForTest.trip("1").withRoute(ROUTE).build();
    final ScheduledTripTimes tripTimes = TripTimesFactory.tripTimes(
      trip,
      List.of(stopTime1, stopTime2),
      new Deduplicator()
    );

    var frequency = new Frequency(trip, FREQUENCY_START, FREQUENCY_END, HEADWAY, true);

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

  private static RoutingTripPattern createTripPattern() {
    var testModel = TransitRepositoryForTest.of();
    var stop1 = testModel.stop("FEED:REGULAR_STOP1", 0, 0).build();
    var stop2 = testModel.stop("FEED:REGULAR_STOP2", 0, 0).build();
    var stopPattern = new StopPattern(
      List.of(stopTime(stop1, 0, 0, 0), stopTime(stop2, 600, 600, 1))
    );
    return TripPattern.of(FeedScopedIdForTestFactory.id("REGULAR_PATTERN"))
      .withRoute(ROUTE)
      .withStopPattern(stopPattern)
      .build()
      .getRoutingTripPattern();
  }

  private static ScheduledTripTimes createTripTimes(
    String tripId,
    int[] arrivalTimes,
    int[] departureTimes
  ) {
    var trip = TransitRepositoryForTest.trip(tripId).withRoute(ROUTE).build();
    return ScheduledTripTimes.of()
      .withTrip(trip)
      .withArrivalTimes(arrivalTimes)
      .withDepartureTimes(departureTimes)
      .build();
  }

  private static BitSet allStops() {
    var result = new BitSet(2);
    result.set(0, 2);
    return result;
  }

  private static StopTime stopTime(
    RegularStop stop,
    int arrivalTime,
    int departureTime,
    int sequence
  ) {
    var stopTime = new StopTime();
    stopTime.setStop(stop);
    stopTime.setArrivalTime(arrivalTime);
    stopTime.setDepartureTime(departureTime);
    stopTime.setStopSequence(sequence);
    return stopTime;
  }
}
