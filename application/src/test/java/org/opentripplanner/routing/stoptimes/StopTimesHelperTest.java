package org.opentripplanner.routing.stoptimes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;

class StopTimesHelperTest {

  static String feedId;
  private static DefaultTransitService transitService;
  private static final LocalDate serviceDate = LocalDate.of(2021, Month.JULY, 26);
  private static FeedScopedId stopId;
  private static TripPattern pattern;

  @BeforeAll
  public static void setUp() throws Exception {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.SIMPLE_GTFS);
    TimetableRepository timetableRepository = model.timetableRepository();
    transitService = new DefaultTransitService(timetableRepository);
    feedId = timetableRepository.getFeedIds().iterator().next();
    stopId = new FeedScopedId(feedId, "J");
    var originalPattern = transitService.getPatternForTrip(
      transitService.getTripForId(new FeedScopedId(feedId, "5.1"))
    );
    var tt = originalPattern.getScheduledTimetable();
    var newTripTimes = tt.getTripTimes(0).copyScheduledTimes();
    newTripTimes.cancelTrip();
    pattern =
      originalPattern
        .copy()
        .withScheduledTimeTableBuilder(builder -> builder.addOrUpdateTripTimes(newTripTimes))
        .build();
    // replace the original pattern by the updated pattern in the transit model
    timetableRepository.addTripPattern(pattern.getId(), pattern);
    timetableRepository.index();
    transitService = new DefaultTransitService(timetableRepository);
  }

  /**
   * Case 0, requested number of departure = 0
   */
  @Test
  void stopTimesForStop_zeroRequestedNumberOfDeparture() {
    var result = StopTimesHelper.stopTimesForStop(
      transitService,
      transitService.getRegularStop(stopId),
      serviceDate.atStartOfDay(transitService.getTimeZone()).toInstant(),
      Duration.ofHours(24),
      0,
      ArrivalDeparture.BOTH,
      true
    );

    assertTrue(result.isEmpty());
  }

  /**
   * Case 1, should find first departure for each pattern when numberOfDepartures is one
   */
  @Test
  void stopTimesForStop_oneDeparture() {
    var result = StopTimesHelper.stopTimesForStop(
      transitService,
      transitService.getRegularStop(stopId),
      serviceDate.atStartOfDay(transitService.getTimeZone()).toInstant(),
      Duration.ofHours(24),
      1,
      ArrivalDeparture.BOTH,
      true
    );

    assertEquals(3, result.stream().mapToLong(s -> s.times.size()).sum());
    var stopTimesForPattern = result
      .stream()
      .filter(s -> s.pattern.getRoute().getId().getId().equals("5"))
      .toList();

    assertEquals(1, stopTimesForPattern.size());

    var stopTimes = stopTimesForPattern.get(0).times;

    assertEquals(1, stopTimes.size());

    var stopTime = stopTimes.get(0);

    assertEquals(stopId, stopTime.getStop().getId());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledArrival());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledDeparture());
    assertEquals(serviceDate, stopTime.getServiceDay());
  }

  /**
   * Case 2, should find all departures for the day, when numberOfDepartures is 10
   */
  @Test
  void stopTimesForStop_allDepartures() {
    var result = StopTimesHelper.stopTimesForStop(
      transitService,
      transitService.getRegularStop(stopId),
      serviceDate.atStartOfDay(transitService.getTimeZone()).toInstant(),
      Duration.ofHours(24),
      10,
      ArrivalDeparture.BOTH,
      true
    );

    assertEquals(5, result.stream().mapToLong(s -> s.times.size()).sum());
    assertTrue(hasCancelledTrips(result));
  }

  @Test
  void stopTimesForStop_noCanceledDepartures() {
    var result = StopTimesHelper.stopTimesForStop(
      transitService,
      transitService.getRegularStop(stopId),
      serviceDate.atStartOfDay(transitService.getTimeZone()).toInstant(),
      Duration.ofHours(24),
      10,
      ArrivalDeparture.BOTH,
      false
    );

    assertEquals(4, result.stream().mapToLong(s -> s.times.size()).sum());
    assertFalse(hasCancelledTrips(result));
  }

  /**
   * Case 3, short search window, no results found
   */
  @Test
  void stopTimesForStop_noDepartures() {
    var result = StopTimesHelper.stopTimesForStop(
      transitService,
      transitService.getRegularStop(stopId),
      serviceDate.atStartOfDay(transitService.getTimeZone()).toInstant(),
      Duration.ofHours(6),
      2,
      ArrivalDeparture.BOTH,
      true
    );

    assertEquals(0, result.stream().mapToLong(s -> s.times.size()).sum());
  }

  /**
   * Case 4, long search window, results found on the next day
   */
  @Test
  void stopTimesForStop_nextDay() {
    var result = StopTimesHelper.stopTimesForStop(
      transitService,
      transitService.getRegularStop(stopId),
      serviceDate.atStartOfDay(transitService.getTimeZone()).plusHours(12).toInstant(),
      Duration.ofHours(36),
      10,
      ArrivalDeparture.BOTH,
      true
    );

    assertEquals(9, result.stream().mapToLong(s -> s.times.size()).sum());

    var stopTimesForPattern = result
      .stream()
      .filter(s -> s.pattern.getRoute().getId().getId().equals("5"))
      .toList();

    assertEquals(1, stopTimesForPattern.size());

    var stopTimes = stopTimesForPattern.get(0).times;

    assertEquals(1, stopTimes.size());

    var stopTime = stopTimes.get(0);

    assertEquals(stopId, stopTime.getStop().getId());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledArrival());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledDeparture());
    assertEquals(serviceDate.plusDays(1), stopTime.getServiceDay());
  }

  /**
   * Case 1, midnight, time range one day, should only find one trip, which is on the same day
   */
  @Test
  void stopTimesForPatternAtStop_oneDayFromMidnight() {
    var stopTimes = StopTimesHelper.stopTimesForPatternAtStop(
      transitService,
      transitService.getRegularStop(stopId),
      pattern,
      serviceDate.atStartOfDay(transitService.getTimeZone()).toInstant(),
      Duration.ofHours(24),
      2,
      ArrivalDeparture.BOTH,
      true
    );

    assertEquals(1, stopTimes.size());

    var stopTime = stopTimes.get(0);

    assertEquals(stopId, stopTime.getStop().getId());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledArrival());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledDeparture());
    assertEquals(serviceDate, stopTime.getServiceDay());
  }

  /**
   * Case 2, midday, time range one day, should only find one trip, which is on the next day
   */
  @Test
  void stopTimesForPatternAtStop_oneDayFromMidday() {
    var stopTimes = StopTimesHelper.stopTimesForPatternAtStop(
      transitService,
      transitService.getRegularStop(stopId),
      pattern,
      serviceDate.atStartOfDay(transitService.getTimeZone()).plusHours(12).toInstant(),
      Duration.ofHours(24),
      2,
      ArrivalDeparture.BOTH,
      true
    );

    assertEquals(1, stopTimes.size());

    var stopTime = stopTimes.get(0);

    assertEquals(stopId, stopTime.getStop().getId());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledArrival());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledDeparture());

    assertEquals(serviceDate.plusDays(1), stopTime.getServiceDay());
  }

  /**
   * Case 3, midnight, time range two days, should only find two trips, on both days
   */
  @Test
  void stopTimesForPatternAtStop_twoDaysFromMidnight() {
    var stopTimes = StopTimesHelper.stopTimesForPatternAtStop(
      transitService,
      transitService.getRegularStop(stopId),
      pattern,
      serviceDate.atStartOfDay(transitService.getTimeZone()).toInstant(),
      Duration.ofHours(48),
      2,
      ArrivalDeparture.BOTH,
      true
    );

    assertEquals(2, stopTimes.size());

    var stopTime = stopTimes.get(0);

    assertEquals(stopId, stopTime.getStop().getId());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledArrival());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledDeparture());
    assertEquals(serviceDate, stopTime.getServiceDay());

    stopTime = stopTimes.get(1);

    assertEquals(stopId, stopTime.getStop().getId());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledArrival());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledDeparture());
    assertEquals(serviceDate.plusDays(1), stopTime.getServiceDay());
  }

  /**
   * Case 1, should find all five departures on this day
   */
  @Test
  void stopTimesForStopServiceDate() {
    var result = StopTimesHelper.stopTimesForStop(
      transitService,
      transitService.getRegularStop(stopId),
      serviceDate,
      ArrivalDeparture.BOTH,
      true
    );

    assertEquals(5, result.stream().mapToLong(s -> s.times.size()).sum());
    var stopTimesForPattern = result
      .stream()
      .filter(s -> s.pattern.getRoute().getId().getId().equals("5"))
      .toList();

    assertEquals(1, stopTimesForPattern.size());

    var stopTimes = stopTimesForPattern.get(0).times;

    assertEquals(1, stopTimes.size());

    var stopTime = stopTimes.get(0);

    assertEquals(stopId, stopTime.getStop().getId());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledArrival());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledDeparture());
    assertEquals(serviceDate, stopTime.getServiceDay());
  }

  boolean hasCancelledTrips(List<StopTimesInPattern> stopTimes) {
    return !stopTimes
      .stream()
      .filter(s ->
        s.times.stream().anyMatch(tripTimeOnDate -> tripTimeOnDate.isCanceledEffectively())
      )
      .findAny()
      .isEmpty();
  }
}
