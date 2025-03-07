package org.opentripplanner.updater.trip;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.trip.TimetableSnapshotManagerTest.SameAssert.NotSame;
import static org.opentripplanner.updater.trip.TimetableSnapshotManagerTest.SameAssert.Same;

import java.time.LocalDate;
import java.time.Month;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.RealTimeTripUpdate;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.updater.TimetableSnapshotParameters;

class TimetableSnapshotManagerTest {

  private static final LocalDate TODAY = LocalDate.of(2024, Month.MAY, 30);
  private static final LocalDate TOMORROW = TODAY.plusDays(1);
  private static final LocalDate YESTERDAY = TODAY.minusDays(1);

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final TripPattern PATTERN = TimetableRepositoryForTest.tripPattern(
    "pattern",
    TimetableRepositoryForTest.route("r1").build()
  )
    .withStopPattern(
      TimetableRepositoryForTest.stopPattern(
        TEST_MODEL.stop("1").build(),
        TEST_MODEL.stop("2").build()
      )
    )
    .build();
  private static final RealTimeTripTimes TRIP_TIMES = RealTimeTripTimes.of(
    ScheduledTripTimes.of()
      .withArrivalTimes("00:00 00:01")
      .withTrip(TimetableRepositoryForTest.trip("trip").build())
      .build()
  );

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
    // We will simulate the clock turning midnight into tomorrow, data on
    // yesterday is candidate to expire
    final AtomicReference<LocalDate> clock = new AtomicReference<>(YESTERDAY);

    var snapshotManager = new TimetableSnapshotManager(
      null,
      TimetableSnapshotParameters.DEFAULT.withPurgeExpiredData(purgeExpiredData),
      clock::get
    );

    var res1 = snapshotManager.updateBuffer(new RealTimeTripUpdate(PATTERN, TRIP_TIMES, YESTERDAY));
    assertTrue(res1.isSuccess());

    snapshotManager.commitTimetableSnapshot(true);
    final TimetableSnapshot snapshotA = snapshotManager.getTimetableSnapshot();

    // Turn the clock to tomorrow
    clock.set(TOMORROW);

    var res2 = snapshotManager.updateBuffer(new RealTimeTripUpdate(PATTERN, TRIP_TIMES, TODAY));
    assertTrue(res2.isSuccess());

    snapshotManager.purgeAndCommit();

    final TimetableSnapshot snapshotB = snapshotManager.getTimetableSnapshot();

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
