package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class TripPatternForDateTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final RegularStop STOP = TEST_MODEL.stop("TEST:STOP", 0, 0).build();
  private static final Route ROUTE = TimetableRepositoryForTest.route("1").build();
  private static final ScheduledTripTimes tripTimes = TripTimesFactory.tripTimes(
    TimetableRepositoryForTest.trip("1").withRoute(ROUTE).build(),
    List.of(new StopTime()),
    new Deduplicator()
  );

  static Stream<Arguments> testCases() {
    return Stream.of(List.of(new FrequencyEntry(new Frequency(), tripTimes)), List.of()).map(
      Arguments::of
    );
  }

  @ParameterizedTest(name = "trip with frequencies {0} should be correctly filtered")
  @MethodSource("testCases")
  void shouldExcludeAndIncludeBasedOnFrequency(List<FrequencyEntry> freqs) {
    var stopTime = new StopTime();
    stopTime.setStop(STOP);
    StopPattern stopPattern = new StopPattern(List.of(stopTime));
    RoutingTripPattern tripPattern = TripPattern.of(TimetableRepositoryForTest.id("P1"))
      .withRoute(ROUTE)
      .withStopPattern(stopPattern)
      .build()
      .getRoutingTripPattern();

    var withFrequencies = new TripPatternForDate(
      tripPattern,
      List.of(tripTimes),
      freqs,
      LocalDate.now()
    );

    assertNull(withFrequencies.newWithFilteredTripTimes(t -> false));
    assertNotNull(withFrequencies.newWithFilteredTripTimes(t -> true));
  }
}
