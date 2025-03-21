package org.opentripplanner.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Objects;
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
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
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

  /**
   * This test checks that the original timetable is given to RaptorTransitDataUpdater for previously
   * added patterns after the buffer is cleared.
   * <p>
   * Refer to bug #6197 for details.
   */
  @Test
  void testRaptorTransitDataUpdaterAfterClear() {
    var resolver = new TimetableSnapshot();

    // make an updated trip
    var pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));
    var trip = pattern.scheduledTripsAsStream().findFirst().orElseThrow();
    var scheduledTimetable = pattern.getScheduledTimetable();
    var updatedTripTimes = Objects.requireNonNull(
      scheduledTimetable.getTripTimes(trip)
    ).copyScheduledTimes();
    for (var i = 0; i < updatedTripTimes.getNumStops(); ++i) {
      updatedTripTimes.updateArrivalDelay(i, 30);
      updatedTripTimes.updateDepartureDelay(i, 30);
    }
    updatedTripTimes.setRealTimeState(RealTimeState.UPDATED);
    var realTimeTripUpdate = new RealTimeTripUpdate(
      pattern,
      updatedTripTimes,
      SERVICE_DATE,
      null,
      false,
      false
    );

    var addedDepartureStopTime = new StopTime();
    var addedArrivalStopTime = new StopTime();
    addedDepartureStopTime.setDepartureTime(0);
    addedDepartureStopTime.setArrivalTime(0);
    addedDepartureStopTime.setStop(RegularStop.of(new FeedScopedId(feedId, "XX"), () -> 0).build());
    addedArrivalStopTime.setDepartureTime(300);
    addedArrivalStopTime.setArrivalTime(300);
    addedArrivalStopTime.setStop(RegularStop.of(new FeedScopedId(feedId, "YY"), () -> 1).build());
    var addedStopTimes = List.of(addedDepartureStopTime, addedArrivalStopTime);
    var addedStopPattern = new StopPattern(addedStopTimes);
    var route = patternIndex.values().stream().findFirst().orElseThrow().getRoute();
    var addedTripPattern = TripPattern.of(new FeedScopedId(feedId, "1.1"))
      .withRoute(route)
      .withStopPattern(addedStopPattern)
      .withCreatedByRealtimeUpdater(true)
      .build();
    var addedTripTimes = TripTimesFactory.tripTimes(
      Trip.of(new FeedScopedId(feedId, "addedTrip")).withRoute(route).build(),
      addedStopTimes,
      new Deduplicator()
    );
    var addedTripUpdate = new RealTimeTripUpdate(
      addedTripPattern,
      addedTripTimes,
      SERVICE_DATE,
      null,
      true,
      false
    );

    var raptorTransitDataUpdater = new RealTimeRaptorTransitDataUpdater(null) {
      int count = 0;

      /**
       * Test that the TransitLayerUpdater receives correct updated timetables upon commit
       * <p>
       * This method is called 3 times.
       * When count = 0, the buffer contains one added and one updated trip, and the timetables
       * should reflect this fact.
       * When count = 1, the buffer is empty, however, this method should still receive the
       * timetables of the previous added and updated patterns in order to restore them to the
       * initial scheduled timetable.
       * When count = 2, the buffer is still empty, and no changes should be made.
       */
      @Override
      public void update(
        Collection<Timetable> updatedTimetables,
        Map<TripPattern, SortedSet<Timetable>> timetables
      ) {
        assertThat(updatedTimetables).hasSize(count == 2 ? 0 : 2);

        updatedTimetables.forEach(timetable -> {
          var timetablePattern = timetable.getPattern();
          assertEquals(SERVICE_DATE, timetable.getServiceDate());
          if (timetablePattern == pattern) {
            if (count == 1) {
              // the timetable for the existing pattern should revert to the original
              assertEquals(scheduledTimetable.getTripTimes(), timetable.getTripTimes());
            } else {
              // the timetable for the existing pattern should contain the modified times
              assertEquals(
                RealTimeState.UPDATED,
                Objects.requireNonNull(timetable.getTripTimes(trip)).getRealTimeState()
              );
            }
          } else if (timetablePattern == addedTripPattern) {
            if (count == 1) {
              // the timetable for the added pattern should be empty after clearing
              assertThat(timetable.getTripTimes()).isEmpty();
            } else {
              // the timetable for the added pattern should contain the times for 1 added trip
              assertThat(timetable.getTripTimes()).hasSize(1);
            }
          } else {
            throw new RuntimeException("unknown pattern passed to transit layer updater");
          }
        });
        ++count;
      }
    };

    resolver.update(realTimeTripUpdate);
    resolver.update(addedTripUpdate);
    resolver.commit(raptorTransitDataUpdater, true);

    resolver.clear(feedId);
    resolver.clear(feedId);
    resolver.clear(feedId);
    assertTrue(resolver.commit(raptorTransitDataUpdater, true).isEmpty());

    resolver.clear(feedId);
    resolver.clear(feedId);
    assertTrue(resolver.commit(raptorTransitDataUpdater, true).isEmpty());
  }

  private static TimetableSnapshot createCommittedSnapshot() {
    TimetableSnapshot timetableSnapshot = new TimetableSnapshot();
    return timetableSnapshot.commit(null, true);
  }
}
