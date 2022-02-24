package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.DateMapper;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;

public class RaptorRoutingRequestTransitDataCreatorTest {

  public static final FeedScopedId TP_ID_1 = new FeedScopedId("F", "1");
  public static final FeedScopedId TP_ID_2 = new FeedScopedId("F", "2");
  public static final FeedScopedId TP_ID_3 = new FeedScopedId("F", "3");

  private static final TripPattern TP = new TripPattern(
          new FeedScopedId("F", "P1"),
          new Route(new FeedScopedId("F", "L1")),
          new StopPattern(List.of())
  );

  @BeforeEach
  public void setup() {
    TP.getRoute().setMode(TransitMode.BUS);
  }


  @Test
  public void testMergeTripPatterns() {
    LocalDate first = LocalDate.of(2019, 3, 30);
    LocalDate second = LocalDate.of(2019, 3, 31);
    LocalDate third = LocalDate.of(2019, 4, 1);

    ZonedDateTime startOfTime = DateMapper.asStartOfService(second, ZoneId.of("Europe/London"));

    List<TripTimes> tripTimes = List.of(createTripTimesForTest());

    int[] stopIndexes = new int[]{0, 1};

    // Total available trip patterns
    TripPatternWithRaptorStopIndexes tripPattern1 = new TripPatternWithId(TP_ID_1, stopIndexes, TP);
    TripPatternWithRaptorStopIndexes tripPattern2 = new TripPatternWithId(TP_ID_2, stopIndexes, TP);
    TripPatternWithRaptorStopIndexes tripPattern3 = new TripPatternWithId(TP_ID_3, stopIndexes, TP);

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

    // Patterns containing trip schedules for all 3 days. Trip schedules for later days are offset in time when requested.
    List<TripPatternForDates> combinedTripPatterns = RaptorRoutingRequestTransitDataCreator.merge(
        startOfTime,
        tripPatternsForDates
    );

    // Get the results
    var r1 = findTripPatternForDate(TP_ID_1, combinedTripPatterns);
    var r2 = findTripPatternForDate(TP_ID_2, combinedTripPatterns);
    var r3 = findTripPatternForDate(TP_ID_3, combinedTripPatterns);

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

  private static TripPatternForDates findTripPatternForDate(
      FeedScopedId patternId, List<TripPatternForDates> list
  ) {
    return list
        .stream()
        .filter(p -> patternId.equals(p.getTripPattern().getId()))
        .findFirst()
        .orElseThrow();
  }

  private TripTimes createTripTimesForTest() {
    StopTime stopTime1 = new StopTime();
    StopTime stopTime2 = new StopTime();

    stopTime1.setDepartureTime(0);
    stopTime2.setArrivalTime(7200);

    return new TripTimes(new Trip(new FeedScopedId("Test", "Test")),
        Arrays.asList(stopTime1, stopTime2),
        new Deduplicator()
    );
  }
}
