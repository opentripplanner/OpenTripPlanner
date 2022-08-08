package org.opentripplanner.routing.stoptimes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

class StopTimesHelperTest {

  static String feedId;
  private static DefaultTransitService transitService;
  private static final LocalDate serviceDate = LocalDate.of(2021, 07, 26);

  @BeforeAll
  public static void setUp() throws Exception {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.FAKE_GTFS);
    TransitModel transitModel = model.transitModel();
    transitService = new DefaultTransitService(transitModel);
    feedId = transitModel.getFeedIds().iterator().next();
  }

  @Test
  void stopTimesForStop() {
    FeedScopedId stopId = new FeedScopedId(feedId, "J");

    // Case 1, should find first departure for each pattern
    var result = StopTimesHelper.stopTimesForStop(
      transitService,
      transitService.getStopForId(stopId),
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

    // Case 2, should find all departures for the day
    result =
      StopTimesHelper.stopTimesForStop(
        transitService,
        transitService.getStopForId(stopId),
        serviceDate.atStartOfDay(transitService.getTimeZone()).toInstant(),
        Duration.ofHours(24),
        10,
        ArrivalDeparture.BOTH,
        true
      );

    assertEquals(5, result.stream().mapToLong(s -> s.times.size()).sum());

    // Case 3, short search window, no results found
    result =
      StopTimesHelper.stopTimesForStop(
        transitService,
        transitService.getStopForId(stopId),
        serviceDate.atStartOfDay(transitService.getTimeZone()).toInstant(),
        Duration.ofHours(6),
        2,
        ArrivalDeparture.BOTH,
        true
      );

    assertEquals(0, result.stream().mapToLong(s -> s.times.size()).sum());

    // Case 4, long search window, results found on the next day
    result =
      StopTimesHelper.stopTimesForStop(
        transitService,
        transitService.getStopForId(stopId),
        serviceDate.atStartOfDay(transitService.getTimeZone()).plusHours(12).toInstant(),
        Duration.ofHours(36),
        10,
        ArrivalDeparture.BOTH,
        true
      );

    assertEquals(9, result.stream().mapToLong(s -> s.times.size()).sum());

    stopTimesForPattern =
      result.stream().filter(s -> s.pattern.getRoute().getId().getId().equals("5")).toList();

    assertEquals(1, stopTimesForPattern.size());

    stopTimes = stopTimesForPattern.get(0).times;

    assertEquals(1, stopTimes.size());

    stopTime = stopTimes.get(0);

    assertEquals(stopId, stopTime.getStop().getId());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledArrival());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledDeparture());
    assertEquals(serviceDate.plusDays(1), stopTime.getServiceDay());
  }

  @Test
  void stopTimesForPatternAtStop() {
    FeedScopedId stopId = new FeedScopedId(feedId, "J");
    TripPattern pattern = transitService.getPatternForTrip(
      transitService.getTripForId(new FeedScopedId(feedId, "5.1"))
    );

    // Case 1, midnight, time range one day, should only find one trip, which is on the same day
    var stopTimes = StopTimesHelper.stopTimesForPatternAtStop(
      transitService,
      transitService.getStopForId(stopId),
      pattern,
      serviceDate.atStartOfDay(transitService.getTimeZone()).toInstant(),
      Duration.ofHours(24),
      2,
      ArrivalDeparture.BOTH
    );

    assertEquals(1, stopTimes.size());

    var stopTime = stopTimes.get(0);

    assertEquals(stopId, stopTime.getStop().getId());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledArrival());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledDeparture());
    assertEquals(serviceDate, stopTime.getServiceDay());

    // Case 2, midday, time range one day, should only find one trip, which is on the next day
    stopTimes =
      StopTimesHelper.stopTimesForPatternAtStop(
        transitService,
        transitService.getStopForId(stopId),
        pattern,
        serviceDate.atStartOfDay(transitService.getTimeZone()).plusHours(12).toInstant(),
        Duration.ofHours(24),
        2,
        ArrivalDeparture.BOTH
      );

    assertEquals(1, stopTimes.size());

    stopTime = stopTimes.get(0);

    assertEquals(stopId, stopTime.getStop().getId());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledArrival());
    assertEquals((8 * 60 + 10) * 60, stopTime.getScheduledDeparture());

    assertEquals(serviceDate.plusDays(1), stopTime.getServiceDay());

    // Case 3, midnight, time range two days, should only find two trips, on both days
    stopTimes =
      StopTimesHelper.stopTimesForPatternAtStop(
        transitService,
        transitService.getStopForId(stopId),
        pattern,
        serviceDate.atStartOfDay(transitService.getTimeZone()).toInstant(),
        Duration.ofHours(48),
        2,
        ArrivalDeparture.BOTH
      );

    assertEquals(2, stopTimes.size());

    stopTime = stopTimes.get(0);

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

  @Test
  void stopTimesForStopServiceDate() {
    FeedScopedId stopId = new FeedScopedId(feedId, "J");

    // Case 1, should find all five departures on this day
    var result = StopTimesHelper.stopTimesForStop(
      transitService,
      transitService.getStopForId(stopId),
      serviceDate,
      ArrivalDeparture.BOTH
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
}
