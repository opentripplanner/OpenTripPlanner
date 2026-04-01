package org.opentripplanner.transit.model.timetable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.TestStopLocation;
import org.opentripplanner.transit.service.TimetableRepository;

public class TimetableSnapshotTest {

  private static final ZoneId TIME_ZONE = ZoneIds.GMT;
  public static final LocalDate SERVICE_DATE = LocalDate.of(2024, 1, 1);
  private static Map<FeedScopedId, TripPattern> patternIndex;
  private static String feedId;

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
    Timetable a = orig.copyOf().withServiceDate(LocalDate.now(TIME_ZONE).minusDays(1)).build();
    Timetable b = orig.copyOf().withServiceDate(LocalDate.now(TIME_ZONE)).build();
    assertTrue(new TimetableSnapshot.SortedTimetableComparator().compare(a, b) < 0);
  }

  @Test
  void testUniqueDirtyTimetablesAfterMultipleUpdates() {
    TimetableSnapshot snapshot = new TimetableSnapshot();
    TripPattern pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));
    Trip trip = pattern.scheduledTripsAsStream().findFirst().orElseThrow();

    RealTimeTripUpdate realTimeTripUpdate = createRealTimeTripUpdate(pattern, trip);

    snapshot.update(realTimeTripUpdate);
    snapshot.update(realTimeTripUpdate);
    assertTrue(snapshot.isDirty());

    AtomicBoolean updateIsCalled = new AtomicBoolean();

    var updateListener = new TimetableSnapshotUpdateListener() {
      @Override
      public void update(
        Collection<Timetable> updatedTimetables,
        Function<FeedScopedId, SortedSet<Timetable>> timetableProvider
      ) {
        updateIsCalled.set(true);
        assertThat(updatedTimetables).hasSize(1);
      }
    };

    snapshot.commit(updateListener, true);

    assertTrue(updateIsCalled.get());
  }

  @Test
  void testCannotUpdateReadOnlyTimetableSnapshot() {
    TimetableSnapshot committedSnapshot = createCommittedSnapshot();
    LocalDate today = LocalDate.now(TIME_ZONE);
    TripPattern pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));
    TripTimes tripTimes = pattern.getScheduledTimetable().getTripTimes().getFirst();
    RealTimeTripUpdate realTimeTripUpdate = RealTimeTripUpdate.of(
      pattern,
      tripTimes,
      today
    ).build();
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
    RealTimeTripUpdate realTimeTripUpdate = createRealTimeTripUpdate(pattern, trip);

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

  @Test
  void testClearWithAllCollections() {
    TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
    RegularStop STOP_A = TEST_MODEL.stop("A").build();
    RegularStop STOP_B = TEST_MODEL.stop("B").build();

    FeedScopedId id = new FeedScopedId(feedId, "1.2");
    TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(id, SERVICE_DATE);
    TripOnServiceDate tripOnServiceDate = TripOnServiceDate.of(
      new FeedScopedId(feedId, "triponservicedateid")
    ).build();
    TestStopLocation testStopLocation = new TestStopLocation(
      new FeedScopedId(feedId, "stoplocationid")
    );
    Route route = TimetableRepositoryForTest.route(new FeedScopedId(feedId, "routeId")).build();
    Trip trip = TimetableRepositoryForTest.trip(feedId, "tripId").build();
    TripPattern tripPattern = TripPattern.of(new FeedScopedId(feedId, "tripPatternId"))
      .withRoute(route)
      .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP_A, STOP_B))
      .withScheduledTimeTableBuilder(builder ->
        builder.addTripTimes(
          ScheduledTripTimes.of().withTrip(trip).withDepartureTimes(new int[] { 0, 1 }).build()
        )
      )
      .build();

    Multimap<Route, TripPattern> realTimeAddedPatternsForRoute = ArrayListMultimap.create();
    realTimeAddedPatternsForRoute.put(route, tripPattern);
    ListMultimap<FeedScopedId, TripOnServiceDate> realTimeAddedReplacedByTripOnServiceDateById =
      ArrayListMultimap.create();
    realTimeAddedReplacedByTripOnServiceDateById.put(id, tripOnServiceDate);
    SetMultimap<StopLocation, TripPattern> patternsForStop = HashMultimap.create();
    patternsForStop.put(testStopLocation, tripPattern);

    // The entries do not necessarily make sense, they are only for testing the clear method.
    TimetableSnapshot snapshot = new TimetableSnapshot(
      new HashMap<>(Map.of(id, ImmutableSortedSet.of())),
      new HashMap<>(Map.of(tripIdAndServiceDate, tripPattern)),
      new HashMap<>(Map.of(id, route)),
      new HashMap<>(Map.of(id, trip)),
      new HashMap<>(Map.of(trip, tripPattern)),
      realTimeAddedPatternsForRoute,
      new HashMap<>(Map.of(id, tripOnServiceDate)),
      realTimeAddedReplacedByTripOnServiceDateById,
      new HashMap<>(Map.of(tripIdAndServiceDate, tripOnServiceDate)),
      patternsForStop,
      false
    );
    assertFalse(snapshot.isEmpty());
    snapshot.clear(id.getFeedId());
    assertTrue(snapshot.isEmpty());
  }

  private static RealTimeTripUpdate createRealTimeTripUpdate(TripPattern pattern, Trip trip) {
    TripTimes updatedTriptimes = TripTimesFactory.tripTimes(
      trip,
      List.of(new StopTime(), new StopTime(), new StopTime()),
      new Deduplicator()
    );
    return RealTimeTripUpdate.of(pattern, updatedTriptimes, SERVICE_DATE)
      .withAddedTripOnServiceDate(
        TripOnServiceDate.of(trip.getId()).withTrip(trip).withServiceDate(SERVICE_DATE).build()
      )
      .withTripCreation(true)
      .withRouteCreation(true)
      .build();
  }

  private static TimetableSnapshot createCommittedSnapshot() {
    TimetableSnapshot timetableSnapshot = new TimetableSnapshot();
    return timetableSnapshot.commit(null, true);
  }
}
