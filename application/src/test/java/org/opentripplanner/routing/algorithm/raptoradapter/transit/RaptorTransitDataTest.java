package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class RaptorTransitDataTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final TripTimes TRIP_TIMES;

  private static final RoutingTripPattern TRIP_PATTERN;

  static {
    var stop = TEST_MODEL.stop("TEST:STOP", 0, 0).build();
    var stopTime = new StopTime();
    stopTime.setStop(stop);
    var stopPattern = new StopPattern(List.of(stopTime));
    var route = TimetableRepositoryForTest.route("1").build();
    TRIP_PATTERN =
      TripPattern
        .of(TimetableRepositoryForTest.id("P1"))
        .withRoute(route)
        .withStopPattern(stopPattern)
        .build()
        .getRoutingTripPattern();
    TRIP_TIMES =
      TripTimesFactory.tripTimes(
        TimetableRepositoryForTest.trip("1").withRoute(route).build(),
        List.of(new StopTime()),
        new Deduplicator()
      );
  }

  @Test
  void testGetTripPatternsRunningOnDateCopy() {
    var date = LocalDate.of(2024, 1, 1);

    var tripPatternForDate = new TripPatternForDate(
      TRIP_PATTERN,
      List.of(TRIP_TIMES),
      List.of(),
      date
    );
    var tripPatterns = List.of(tripPatternForDate);
    var raptorTransitData = new RaptorTransitData(
      Map.of(date, tripPatterns),
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
    var runningOnDate = raptorTransitData.getTripPatternsRunningOnDateCopy(date);
    assertEquals(1, runningOnDate.size());
    assertEquals(tripPatterns, runningOnDate);
    assertFalse(tripPatterns == runningOnDate);
    assertEquals(0, raptorTransitData.getTripPatternsRunningOnDateCopy(date.minusDays(1)).size());
    assertEquals(0, raptorTransitData.getTripPatternsRunningOnDateCopy(date.plusDays(1)).size());
  }

  @Test
  void testGetTripPatternsForRunningDate() {
    var date = LocalDate.of(2024, 1, 1);

    var tripPatternForDate = new TripPatternForDate(
      TRIP_PATTERN,
      List.of(TRIP_TIMES),
      List.of(),
      date
    );
    var tripPatterns = List.of(tripPatternForDate);
    var raptorTransitData = new RaptorTransitData(
      Map.of(date, tripPatterns),
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
    var runningOnDate = raptorTransitData.getTripPatternsForRunningDate(date);
    assertEquals(1, runningOnDate.size());
    assertEquals(tripPatterns, runningOnDate);
    assertTrue(tripPatterns == runningOnDate);
    assertEquals(0, raptorTransitData.getTripPatternsForRunningDate(date.minusDays(1)).size());
    assertEquals(0, raptorTransitData.getTripPatternsForRunningDate(date.plusDays(1)).size());
  }

  @Test
  void testGetTripPatternsOnServiceDateCopyWithSameRunningAndServiceDate() {
    var date = LocalDate.of(2024, 1, 1);

    var tripPatternForDate = new TripPatternForDate(
      TRIP_PATTERN,
      List.of(TRIP_TIMES),
      List.of(),
      date
    );
    var raptorTransitData = new RaptorTransitData(
      Map.of(date, List.of(tripPatternForDate)),
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
    var startingOnDate = raptorTransitData.getTripPatternsOnServiceDateCopy(date);
    assertEquals(1, startingOnDate.size());
    assertEquals(tripPatternForDate, startingOnDate.getFirst());
    assertEquals(0, raptorTransitData.getTripPatternsOnServiceDateCopy(date.minusDays(1)).size());
    assertEquals(0, raptorTransitData.getTripPatternsOnServiceDateCopy(date.plusDays(1)).size());
  }

  @Test
  void testGetTripPatternsOnServiceDateCopyWithServiceRunningAfterMidnight() {
    var runningDate = LocalDate.of(2024, 1, 1);
    var serviceDate = runningDate.minusDays(1);

    var tripPatternForDate = new TripPatternForDate(
      TRIP_PATTERN,
      List.of(TRIP_TIMES),
      List.of(),
      serviceDate
    );
    var raptorTransitData = new RaptorTransitData(
      Map.of(runningDate, List.of(tripPatternForDate)),
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
    var startingOnDate = raptorTransitData.getTripPatternsOnServiceDateCopy(serviceDate);
    // starting date should be determined by service date, not running date which refers to the
    // normal calendar date that the trip pattern is running on
    assertEquals(1, startingOnDate.size());
    assertEquals(tripPatternForDate, startingOnDate.getFirst());
    assertEquals(0, raptorTransitData.getTripPatternsOnServiceDateCopy(runningDate).size());
  }

  @Test
  void testGetTripPatternsOnServiceDateCopyWithServiceRunningBeforeAndAfterMidnight() {
    // This is same as the service date
    var firstRunningDate = LocalDate.of(2024, 1, 1);
    var secondRunningDate = firstRunningDate.plusDays(1);

    var tripPatternForDate = new TripPatternForDate(
      TRIP_PATTERN,
      List.of(TRIP_TIMES),
      List.of(),
      firstRunningDate
    );
    var raptorTransitData = new RaptorTransitData(
      Map.ofEntries(
        entry(firstRunningDate, List.of(tripPatternForDate)),
        entry(secondRunningDate, List.of(tripPatternForDate))
      ),
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
    var startingOnDate = raptorTransitData.getTripPatternsOnServiceDateCopy(firstRunningDate);
    // Transit layer indexes trip patterns by running date and to get trip patterns for certain
    // service date, we need to look up the trip patterns for the next running date as well, but
    // we don't want to return duplicates
    assertEquals(1, startingOnDate.size());
    assertEquals(tripPatternForDate, startingOnDate.getFirst());
    assertEquals(0, raptorTransitData.getTripPatternsOnServiceDateCopy(secondRunningDate).size());
  }
}
