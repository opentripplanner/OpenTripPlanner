package org.opentripplanner.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
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
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.BackwardsDelayPropagationType;

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
  public void testResolve() {
    LocalDate today = LocalDate.now(timeZone);
    LocalDate yesterday = today.minusDays(1);
    LocalDate tomorrow = today.plusDays(1);
    TripPattern pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));
    TimetableSnapshot resolver = new TimetableSnapshot();

    Timetable scheduled = resolver.resolve(pattern, today);
    assertEquals(scheduled, resolver.resolve(pattern, null));

    TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.SCHEDULED);

    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);

    var stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(
      TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED
    );
    stopTimeUpdateBuilder.setDeparture(TripUpdate.StopTimeEvent.newBuilder().setDelay(2).build());

    TripUpdate tripUpdate = tripUpdateBuilder.build();

    // add a new timetable for today
    updateResolver(resolver, pattern, tripUpdate, today);
    Timetable forNow = resolver.resolve(pattern, today);
    assertEquals(scheduled, resolver.resolve(pattern, yesterday));
    assertNotSame(scheduled, forNow);
    assertEquals(scheduled, resolver.resolve(pattern, tomorrow));
    assertEquals(scheduled, resolver.resolve(pattern, null));

    // add a new timetable for yesterday
    updateResolver(resolver, pattern, tripUpdate, yesterday);
    Timetable forYesterday = resolver.resolve(pattern, yesterday);
    assertNotSame(scheduled, forYesterday);
    assertNotSame(scheduled, forNow);
    assertEquals(scheduled, resolver.resolve(pattern, tomorrow));
    assertEquals(scheduled, resolver.resolve(pattern, null));
  }

  @Test
  public void testUpdate() {
    LocalDate today = LocalDate.now(timeZone);
    LocalDate yesterday = today.minusDays(1);
    TripPattern pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));

    TimetableSnapshot resolver = new TimetableSnapshot();
    Timetable origNow = resolver.resolve(pattern, today);

    TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.SCHEDULED);

    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);

    var stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(
      TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED
    );
    stopTimeUpdateBuilder.setDeparture(TripUpdate.StopTimeEvent.newBuilder().setDelay(5).build());

    TripUpdate tripUpdate = tripUpdateBuilder.build();

    // new timetable for today
    updateResolver(resolver, pattern, tripUpdate, today);
    Timetable updatedNow = resolver.resolve(pattern, today);
    assertNotSame(origNow, updatedNow);

    // a new timetable instance is created for today
    updateResolver(resolver, pattern, tripUpdate, today);
    assertNotEquals(updatedNow, resolver.resolve(pattern, today));

    // create new timetable for tomorrow
    updateResolver(resolver, pattern, tripUpdate, yesterday);
    assertNotSame(origNow, resolver.resolve(pattern, yesterday));
    assertNotSame(updatedNow, resolver.resolve(pattern, yesterday));

    // exception if we try to modify a snapshot
    TimetableSnapshot snapshot = resolver.commit();
    assertThrows(
      ConcurrentModificationException.class,
      () -> {
        updateResolver(snapshot, pattern, tripUpdate, yesterday);
      }
    );
  }

  @Test
  public void testCommit() {
    assertThrows(
      ConcurrentModificationException.class,
      () -> {
        LocalDate today = LocalDate.now(timeZone);
        LocalDate yesterday = today.minusDays(1);
        TripPattern pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));

        TimetableSnapshot resolver = new TimetableSnapshot();

        // only return a new snapshot if there are changes
        TimetableSnapshot snapshot = resolver.commit();
        assertNull(snapshot);

        TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.SCHEDULED);

        TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        var stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setScheduleRelationship(
          TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setDeparture(
          TripUpdate.StopTimeEvent.newBuilder().setDelay(10).build()
        );

        TripUpdate tripUpdate = tripUpdateBuilder.build();

        // add a new timetable for today, commit, and everything should match
        assertTrue(updateResolver(resolver, pattern, tripUpdate, today).isSuccess());
        snapshot = resolver.commit();
        assertEquals(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
        assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

        // add a new timetable for today, don't commit, and everything should not match
        assertTrue(updateResolver(resolver, pattern, tripUpdate, today).isSuccess());
        assertNotSame(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
        assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

        // add a new timetable for today, on another day, and things should still not match
        assertTrue(updateResolver(resolver, pattern, tripUpdate, yesterday).isSuccess());
        assertNotSame(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

        // commit, and things should match
        snapshot = resolver.commit();
        assertEquals(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
        assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

        // exception if we try to commit to a snapshot
        snapshot.commit();
      }
    );
  }

  @Test
  public void testPurge() {
    LocalDate today = LocalDate.now(timeZone);
    LocalDate yesterday = today.minusDays(1);
    TripPattern pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));

    TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.SCHEDULED);

    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);

    var stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(
      TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED
    );
    stopTimeUpdateBuilder.setDeparture(TripUpdate.StopTimeEvent.newBuilder().setDelay(15).build());

    TripUpdate tripUpdate = tripUpdateBuilder.build();

    TimetableSnapshot resolver = new TimetableSnapshot();
    updateResolver(resolver, pattern, tripUpdate, today);
    updateResolver(resolver, pattern, tripUpdate, yesterday);

    assertNotSame(resolver.resolve(pattern, yesterday), resolver.resolve(pattern, null));
    assertNotSame(resolver.resolve(pattern, today), resolver.resolve(pattern, null));

    assertNotNull(resolver.commit());
    assertFalse(resolver.isDirty());

    assertTrue(resolver.purgeExpiredData(yesterday));
    assertFalse(resolver.purgeExpiredData(yesterday));

    assertEquals(resolver.resolve(pattern, yesterday), resolver.resolve(pattern, null));
    assertNotSame(resolver.resolve(pattern, today), resolver.resolve(pattern, null));

    assertNull(resolver.commit());
    assertFalse(resolver.isDirty());
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
    assertThrows(
      ConcurrentModificationException.class,
      () -> committedSnapshot.update(realTimeTripUpdate)
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
    assertThrows(
      ConcurrentModificationException.class,
      () -> committedSnapshot.purgeExpiredData(null)
    );
  }

  @Test
  void testCannotRevertReadOnlyTimetableSnapshot() {
    TimetableSnapshot committedSnapshot = createCommittedSnapshot();
    assertThrows(
      ConcurrentModificationException.class,
      () -> committedSnapshot.revertTripToScheduledTripPattern(null, null)
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

  private Result<?, UpdateError> updateResolver(
    TimetableSnapshot resolver,
    TripPattern pattern,
    TripUpdate tripUpdate,
    LocalDate serviceDate
  ) {
    var result = pattern
      .getScheduledTimetable()
      .createUpdatedTripTimesFromGTFSRT(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );
    if (result.isSuccess()) {
      return resolver.update(
        new RealTimeTripUpdate(pattern, result.successValue().getTripTimes(), serviceDate)
      );
    }
    throw new RuntimeException("createUpdatedTripTimes returned an error: " + result);
  }
}
