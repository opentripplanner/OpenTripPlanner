package org.opentripplanner.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RealTimeRaptorTransitDataUpdater;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.TimetableRepository;

public class TimetableSnapshotTest {

  private static final ZoneId timeZone = ZoneIds.GMT;
  public static final LocalDate SERVICE_DATE = LocalDate.of(2024, 1, 1);
  private static Map<FeedScopedId, TripPattern> patternIndex;
  static String feedId;

  @BeforeAll
  public static void setUp() throws Exception {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.SIMPLE_GTFS);
    TimetableRepository timetableRepository = model.timetableRepository();

    feedId = timetableRepository.getFeedIds().iterator().next();

    patternIndex = new HashMap<>();
    for (TripPattern tripPattern : timetableRepository.getAllTripPatterns()) {
      tripPattern
        .scheduledTripsAsStream()
        .forEach(trip -> patternIndex.put(trip.getId(), tripPattern));
    }
  }

  @Test
  public void testCompare() {
    Timetable orig = Timetable.of().build();
    Timetable a = orig.copyOf().withServiceDate(LocalDate.now(timeZone).minusDays(1)).build();
    Timetable b = orig.copyOf().withServiceDate(LocalDate.now(timeZone)).build();
    assertTrue(new TimetableSnapshot.SortedTimetableComparator().compare(a, b) < 0);
  }

  @Test
  void testUniqueDirtyTimetablesAfterMultipleUpdates() {
    TimetableSnapshot snapshot = new TimetableSnapshot();
    TripPattern pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));
    Trip trip = pattern.scheduledTripsAsStream().findFirst().orElseThrow();

    TripTimes updatedTriptimes = TripTimesFactory.tripTimes(
      trip,
      List.of(new StopTime()),
      new Deduplicator()
    );
    RealTimeTripUpdate realTimeTripUpdate = new RealTimeTripUpdate(
      pattern,
      updatedTriptimes,
      SERVICE_DATE,
      TripOnServiceDate.of(trip.getId()).withTrip(trip).withServiceDate(SERVICE_DATE).build(),
      true,
      true
    );

    snapshot.update(realTimeTripUpdate);
    snapshot.update(realTimeTripUpdate);
    assertTrue(snapshot.isDirty());

    AtomicBoolean updateIsCalled = new AtomicBoolean();

    RealTimeRaptorTransitDataUpdater raptorTransitData = new RealTimeRaptorTransitDataUpdater(
      null
    ) {
      @Override
      public void update(
        Collection<Timetable> updatedTimetables,
        Map<TripPattern, SortedSet<Timetable>> timetables
      ) {
        updateIsCalled.set(true);
        assertThat(updatedTimetables).hasSize(1);
        assertThat(timetables).hasSize(1);
      }
    };

    snapshot.commit(raptorTransitData, true);

    assertTrue(updateIsCalled.get());
  }

  @Test
  void testCannotUpdateReadOnlyTimetableSnapshot() {
    TimetableSnapshot committedSnapshot = createCommittedSnapshot();
    LocalDate today = LocalDate.now(timeZone);
    TripPattern pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));
    TripTimes tripTimes = pattern.getScheduledTimetable().getTripTimes().getFirst();
    RealTimeTripUpdate realTimeTripUpdate = new RealTimeTripUpdate(pattern, tripTimes, today);
    assertThrows(ConcurrentModificationException.class, () ->
      committedSnapshot.update(realTimeTripUpdate)
    );
  }

  @Test
  void testCannotCommitReadOnlyTimetableSnapshot() {
    TimetableSnapshot committedSnapshot = createCommittedSnapshot();
    assertThrows(ConcurrentModificationException.class, () -> committedSnapshot.commit(null, true));
  }

  @Test
  void testCannotClearReadOnlyTimetableSnapshot() {
    TimetableSnapshot committedSnapshot = createCommittedSnapshot();
    assertThrows(ConcurrentModificationException.class, () -> committedSnapshot.clear(null));
  }

  @Test
  void testCannotPurgeReadOnlyTimetableSnapshot() {
    TimetableSnapshot committedSnapshot = createCommittedSnapshot();
    assertThrows(ConcurrentModificationException.class, () ->
      committedSnapshot.purgeExpiredData(null)
    );
  }

  @Test
  void testCannotRevertReadOnlyTimetableSnapshot() {
    TimetableSnapshot committedSnapshot = createCommittedSnapshot();
    assertThrows(ConcurrentModificationException.class, () ->
      committedSnapshot.revertTripToScheduledTripPattern(null, null)
    );
  }

  @Test
  void testClear() {
    TimetableSnapshot snapshot = new TimetableSnapshot();
    TripPattern pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));
    Trip trip = pattern.scheduledTripsAsStream().findFirst().orElseThrow();

    TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(
      trip.getId(),
      SERVICE_DATE
    );
    TripTimes updatedTriptimes = TripTimesFactory.tripTimes(
      trip,
      List.of(new StopTime()),
      new Deduplicator()
    );
    RealTimeTripUpdate realTimeTripUpdate = new RealTimeTripUpdate(
      pattern,
      updatedTriptimes,
      SERVICE_DATE,
      TripOnServiceDate.of(trip.getId()).withTrip(trip).withServiceDate(SERVICE_DATE).build(),
      true,
      true
    );

    snapshot.update(realTimeTripUpdate);

    assertNotNull(snapshot.getRealTimeAddedTrip(trip.getId()));
    assertNotNull(snapshot.getRealTimeAddedPatternForTrip(trip));
    assertFalse(snapshot.getRealTimeAddedPatternForRoute(pattern.getRoute()).isEmpty());
    assertNotNull(snapshot.getRealTimeAddedTripOnServiceDateById(trip.getId()));
    assertNotNull(snapshot.getRealTimeAddedTripOnServiceDateForTripAndDay(tripIdAndServiceDate));
    assertNotNull(snapshot.getRealtimeAddedRoute(pattern.getRoute().getId()));

    snapshot.clear(trip.getId().getFeedId());

    assertNull(snapshot.getRealTimeAddedTrip(trip.getId()));
    assertNull(snapshot.getRealTimeAddedPatternForTrip(trip));
    assertNull(snapshot.getRealTimeAddedTripOnServiceDateById(trip.getId()));
    assertNull(snapshot.getRealTimeAddedTripOnServiceDateForTripAndDay(tripIdAndServiceDate));
    assertNull(snapshot.getRealtimeAddedRoute(pattern.getRoute().getId()));
    assertTrue(snapshot.getRealTimeAddedPatternForRoute(pattern.getRoute()).isEmpty());

    snapshot.update(realTimeTripUpdate);
    snapshot.clear("another feed id");

    assertNotNull(snapshot.getRealTimeAddedTrip(trip.getId()));
    assertNotNull(snapshot.getRealTimeAddedPatternForTrip(trip));
    assertFalse(snapshot.getRealTimeAddedPatternForRoute(pattern.getRoute()).isEmpty());
    assertNotNull(snapshot.getRealTimeAddedTripOnServiceDateById(trip.getId()));
    assertNotNull(snapshot.getRealTimeAddedTripOnServiceDateForTripAndDay(tripIdAndServiceDate));
    assertNotNull(snapshot.getRealtimeAddedRoute(pattern.getRoute().getId()));
  }

  private static TimetableSnapshot createCommittedSnapshot() {
    TimetableSnapshot timetableSnapshot = new TimetableSnapshot();
    return timetableSnapshot.commit(null, true);
  }
}
