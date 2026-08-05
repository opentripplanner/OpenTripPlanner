package org.opentripplanner.transit.repository;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.transit.repository.TimetableRepositoryLifecycleTest.SameAssert.NotSame;
import static org.opentripplanner.transit.repository.TimetableRepositoryLifecycleTest.SameAssert.Same;

import java.time.LocalDate;
import java.time.Month;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitDataTestFactory;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.calendar.DefaultTripCalendars;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Tests for {@link TimetableRepositoryLifecycle}, specifically the purge-expired-data behavior
 * triggered during {@link TimetableRepositoryLifecycle#freeze(TimetableRepository)}.
 */
class TimetableRepositoryLifecycleTest {

  private static final LocalDate TODAY = LocalDate.of(2024, Month.MAY, 30);
  private static final LocalDate TOMORROW = TODAY.plusDays(1);
  private static final LocalDate YESTERDAY = TODAY.minusDays(1);

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final TripPattern PATTERN = TransitRepositoryForTest.tripPattern(
    "pattern",
    TransitRepositoryForTest.route("r1").build()
  )
    .withStopPattern(
      TransitRepositoryForTest.stopPattern(
        TEST_MODEL.stop("1").build(),
        TEST_MODEL.stop("2").build()
      )
    )
    .build();
  private static final TripTimes TRIP_TIMES = ScheduledTripTimes.of()
    .withArrivalTimes("00:00 00:01")
    .withTrip(TransitRepositoryForTest.trip("trip").build())
    .build();

  enum SameAssert {
    Same {
      public void test(Object a, Object b) {
        assertSame(a, b);
      }
    },
    NotSame {
      public void test(Object a, Object b) {
        assertNotSame(a, b);
      }
    };

    abstract void test(Object a, Object b);

    SameAssert not() {
      return this == Same ? NotSame : Same;
    }
  }

  static Stream<Arguments> purgeExpiredDataTestCases() {
    return Stream.of(
      // purgeExpiredData   || snapshots PatternSnapshotA  PatternSnapshotB
      Arguments.of(Boolean.TRUE, NotSame, NotSame),
      Arguments.of(Boolean.FALSE, NotSame, Same)
    );
  }

  @ParameterizedTest(name = "purgeExpired: {0} ||  {1}  {2}")
  @MethodSource("purgeExpiredDataTestCases")
  public void testPurgeExpiredData(
    boolean purgeExpiredData,
    SameAssert expSnapshots,
    SameAssert expPatternAeqB
  ) {
    final AtomicReference<LocalDate> clock = new AtomicReference<>(YESTERDAY);

    var buffer = new DefaultTimetableRepository(
      RaptorTransitDataTestFactory.empty(),
      new DefaultTripCalendars()
    );
    var lifecycle = new TimetableRepositoryLifecycle(buffer, purgeExpiredData, clock::get);

    // Add data for YESTERDAY, freeze to produce snapshot A
    buffer.update(RealTimeTripUpdate.of(PATTERN, TRIP_TIMES, YESTERDAY).build());
    var snapshotA = lifecycle.freeze(buffer);

    // Advance the clock to TOMORROW and add data for TODAY
    clock.set(TOMORROW);
    buffer.update(RealTimeTripUpdate.of(PATTERN, TRIP_TIMES, TODAY).build());

    // Freeze again — if purge is enabled, YESTERDAY data should be purged
    var snapshotB = lifecycle.freeze(buffer);

    expSnapshots.test(snapshotA, snapshotB);
    expPatternAeqB.test(
      snapshotA.resolve(PATTERN, YESTERDAY),
      snapshotB.resolve(PATTERN, YESTERDAY)
    );
    expPatternAeqB
      .not()
      .test(snapshotB.resolve(PATTERN, null), snapshotB.resolve(PATTERN, YESTERDAY));

    // Expect the same results regardless of the config for these
    assertNotSame(snapshotA.resolve(PATTERN, null), snapshotA.resolve(PATTERN, YESTERDAY));
    assertSame(snapshotA.resolve(PATTERN, null), snapshotB.resolve(PATTERN, null));
  }
}
