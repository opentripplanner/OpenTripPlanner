package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriorityService;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class RaptorRoutingRequestTransitDataCreatorTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  public static final FeedScopedId TP_ID_1 = id("1");
  public static final FeedScopedId TP_ID_2 = id("2");
  public static final FeedScopedId TP_ID_3 = id("3");

  @Test
  public void testMergeTripPatterns() {
    LocalDate first = LocalDate.of(2019, 3, 30);
    LocalDate second = LocalDate.of(2019, 3, 31);
    LocalDate third = LocalDate.of(2019, 4, 1);

    ZonedDateTime startOfTime = ServiceDateUtils.asStartOfService(second, ZoneIds.LONDON);

    List<TripTimes> tripTimes = List.of(createTripTimesForTest());

    // Total available trip patterns
    RoutingTripPattern tripPattern1 = createTripPattern(TP_ID_1);
    RoutingTripPattern tripPattern2 = createTripPattern(TP_ID_2);
    RoutingTripPattern tripPattern3 = createTripPattern(TP_ID_3);

    List<TripPatternForDate> tripPatternsForDates = new ArrayList<>();

    // TripPatterns valid for 1st day in search range
    tripPatternsForDates.add(new TripPatternForDate(tripPattern1, tripTimes, List.of(), first));
    tripPatternsForDates.add(new TripPatternForDate(tripPattern2, tripTimes, List.of(), first));
    tripPatternsForDates.add(new TripPatternForDate(tripPattern3, tripTimes, List.of(), first));

    // TripPatterns valid for 2nd day in search range
    tripPatternsForDates.add(new TripPatternForDate(tripPattern2, tripTimes, List.of(), second));
    tripPatternsForDates.add(new TripPatternForDate(tripPattern3, tripTimes, List.of(), second));

    // TripPatterns valid for 3rd day in search range
    tripPatternsForDates.add(new TripPatternForDate(tripPattern1, tripTimes, List.of(), third));
    tripPatternsForDates.add(new TripPatternForDate(tripPattern3, tripTimes, List.of(), third));

    var noOpFilter = DefaultTransitDataProviderFilter.ofRequest(RouteRequest.defaultValue());

    // Patterns containing trip schedules for all 3 days. Trip schedules for later days are offset
    // in time when requested.
    List<TripPatternForDates> combinedTripPatterns = RaptorRoutingRequestTransitDataCreator.merge(
      startOfTime,
      tripPatternsForDates,
      noOpFilter,
      TransitGroupPriorityService.empty()
    );

    // Get the results
    var r1 = findTripPatternForDate(tripPattern1.patternIndex(), combinedTripPatterns);
    var r2 = findTripPatternForDate(tripPattern2.patternIndex(), combinedTripPatterns);
    var r3 = findTripPatternForDate(tripPattern3.patternIndex(), combinedTripPatterns);

    // Check the number of trip schedules available for each pattern after combining dates in the search range
    assertEquals(2, r1.numberOfTripSchedules());
    assertEquals(2, r2.numberOfTripSchedules());
    assertEquals(3, r3.numberOfTripSchedules());

    // Verify that the per-day offsets were calculated correctly
    //   DST - Clocks go forward on March 31st
    assertEquals(-82800, ((TripScheduleWithOffset) r3.getTripSchedule(0)).getSecondsOffset());
    assertEquals(0, ((TripScheduleWithOffset) r3.getTripSchedule(1)).getSecondsOffset());
    assertEquals(86400, ((TripScheduleWithOffset) r3.getTripSchedule(2)).getSecondsOffset());
  }

  @Test
  public void testCreateTripPatterns() {
    var date = LocalDate.of(2025, 10, 10);
    List<TripTimes> tripTimes = List.of(
      ScheduledTripTimes.of()
        .withTrip(TimetableRepositoryForTest.trip("Test").build())
        .withDepartureTimes("23:45 23:55")
        .build(),
      ScheduledTripTimes.of()
        .withTrip(TimetableRepositoryForTest.trip("Test").build())
        .withDepartureTimes("23:47 23:57")
        .build(),
      ScheduledTripTimes.of()
        .withTrip(TimetableRepositoryForTest.trip("Test").build())
        .withDepartureTimes("23:49 23:59")
        .build(),
      ScheduledTripTimes.of()
        .withTrip(TimetableRepositoryForTest.trip("Test").build())
        .withDepartureTimes("23:51 24:01")
        .build(),
      ScheduledTripTimes.of()
        .withTrip(TimetableRepositoryForTest.trip("Test").build())
        .withDepartureTimes("23:53 24:03")
        .build()
    );
    var tripPattern = createTripPattern(TP_ID_1);
    Map<LocalDate, List<TripPatternForDate>> tripPatternsRunningOnDate = new HashMap<>();
    for (var i = -5; i <= 5; ++i) {
      var tripPatternForDate = new TripPatternForDate(
        tripPattern,
        tripTimes,
        List.of(),
        date.minusDays(i)
      );
      for (var runningDate : tripPatternForDate.getRunningPeriodDates()) {
        tripPatternsRunningOnDate.putIfAbsent(runningDate, new ArrayList<>());
        tripPatternsRunningOnDate.get(runningDate).add(tripPatternForDate);
      }
    }

    var subject = new RaptorRoutingRequestTransitDataCreator(
      new RaptorTransitData(
        tripPatternsRunningOnDate,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null
      ),
      ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneIds.UTC)
    );
    var result = subject.createTripPatterns(
      2,
      0,
      DefaultTransitDataProviderFilter.ofRequest(RouteRequest.defaultValue()),
      TransitGroupPriorityService.empty()
    );
    var expectedDates = List.of(
      LocalDate.of(2025, 10, 7),
      LocalDate.of(2025, 10, 8),
      LocalDate.of(2025, 10, 9),
      LocalDate.of(2025, 10, 10)
    );
    TripPatternForDates resultPattern = result.getFirst();
    var iterator = resultPattern.tripPatternForDatesIndexIterator(true);
    for (var item : expectedDates) {
      assertTrue(iterator.hasNext());
      assertEquals(item, resultPattern.tripPatternForDate(iterator.next()).getServiceDate());
    }
    assertFalse(iterator.hasNext());
  }

  private static TripPatternForDates findTripPatternForDate(
    int patternIndex,
    List<TripPatternForDates> list
  ) {
    return list
      .stream()
      .filter(p -> patternIndex == p.getTripPattern().patternIndex())
      .findFirst()
      .orElseThrow();
  }

  private TripTimes createTripTimesForTest() {
    return ScheduledTripTimes.of()
      .withTrip(TimetableRepositoryForTest.trip("Test").build())
      .withDepartureTimes("00:00 02:00")
      .build();
  }

  /**
   * Utility function to create bare minimum of valid StopTime
   *
   * @return StopTime instance
   */
  private static StopTime createStopTime() {
    var st = new StopTime();
    st.setStop(TEST_MODEL.stop("Stop:1", 0.0, 0.0).build());
    return st;
  }

  private static RoutingTripPattern createTripPattern(FeedScopedId id) {
    return TripPattern.of(id)
      .withRoute(TimetableRepositoryForTest.route("1").withMode(TransitMode.BUS).build())
      .withStopPattern(new StopPattern(List.of(createStopTime(), createStopTime())))
      .build()
      .getRoutingTripPattern();
  }
}
