package org.opentripplanner.updater.trip.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.RealTimeTripUpdate;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.gtfs.model.TripUpdate;

/**
 * The test class is called legacy not because it tests a legacy feature but because the test
 * setup is not how we would write a test in 2025: it tests several classes and reads a full GTFS
 * feed from disk. It's also very hard to follow. All in all, I would say its usefulness is
 * questionable.
 */
public class LegacyTimetableSnapshotIntegrationTest {

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

    GtfsRealtime.TripUpdate.Builder tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);

    var stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(
      GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED
    );
    stopTimeUpdateBuilder.setDeparture(
      GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(2).build()
    );

    GtfsRealtime.TripUpdate tripUpdate = tripUpdateBuilder.build();

    // add a new timetable for today
    updateSnapshot(resolver, pattern, tripUpdate, today);
    Timetable forNow = resolver.resolve(pattern, today);
    assertEquals(scheduled, resolver.resolve(pattern, yesterday));
    assertNotSame(scheduled, forNow);
    assertEquals(scheduled, resolver.resolve(pattern, tomorrow));
    assertEquals(scheduled, resolver.resolve(pattern, null));

    // add a new timetable for yesterday
    updateSnapshot(resolver, pattern, tripUpdate, yesterday);
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

    GtfsRealtime.TripUpdate.Builder tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);

    var stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(
      GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED
    );
    stopTimeUpdateBuilder.setDeparture(
      GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(5).build()
    );

    GtfsRealtime.TripUpdate tripUpdate = tripUpdateBuilder.build();

    // new timetable for today
    updateSnapshot(resolver, pattern, tripUpdate, today);
    Timetable updatedNow = resolver.resolve(pattern, today);
    assertNotSame(origNow, updatedNow);

    // a new timetable instance is created for today
    updateSnapshot(resolver, pattern, tripUpdate, today);
    assertNotEquals(updatedNow, resolver.resolve(pattern, today));

    // create new timetable for tomorrow
    updateSnapshot(resolver, pattern, tripUpdate, yesterday);
    assertNotSame(origNow, resolver.resolve(pattern, yesterday));
    assertNotSame(updatedNow, resolver.resolve(pattern, yesterday));

    // exception if we try to modify a snapshot
    TimetableSnapshot snapshot = resolver.commit();
    assertThrows(ConcurrentModificationException.class, () -> {
      updateSnapshot(snapshot, pattern, tripUpdate, yesterday);
    });
  }

  @Test
  public void testCommit() {
    assertThrows(ConcurrentModificationException.class, () -> {
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

      GtfsRealtime.TripUpdate.Builder tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder();

      tripUpdateBuilder.setTrip(tripDescriptorBuilder);

      var stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
      stopTimeUpdateBuilder.setStopSequence(2);
      stopTimeUpdateBuilder.setScheduleRelationship(
        GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED
      );
      stopTimeUpdateBuilder.setDeparture(
        GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(10).build()
      );

      GtfsRealtime.TripUpdate tripUpdate = tripUpdateBuilder.build();

      // add a new timetable for today, commit, and everything should match
      assertTrue(updateSnapshot(resolver, pattern, tripUpdate, today).isSuccess());
      snapshot = resolver.commit();
      assertEquals(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
      assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

      // add a new timetable for today, don't commit, and everything should not match
      assertTrue(updateSnapshot(resolver, pattern, tripUpdate, today).isSuccess());
      assertNotSame(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
      assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

      // add a new timetable for today, on another day, and things should still not match
      assertTrue(updateSnapshot(resolver, pattern, tripUpdate, yesterday).isSuccess());
      assertNotSame(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

      // commit, and things should match
      snapshot = resolver.commit();
      assertEquals(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
      assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

      // exception if we try to commit to a snapshot
      snapshot.commit();
    });
  }

  @Test
  public void testPurge() {
    LocalDate today = LocalDate.now(timeZone);
    LocalDate yesterday = today.minusDays(1);
    TripPattern pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));

    TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.SCHEDULED);

    GtfsRealtime.TripUpdate.Builder tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);

    var stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(
      GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED
    );
    stopTimeUpdateBuilder.setDeparture(
      GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder().setDelay(15).build()
    );

    GtfsRealtime.TripUpdate tripUpdate = tripUpdateBuilder.build();

    TimetableSnapshot resolver = new TimetableSnapshot();
    updateSnapshot(resolver, pattern, tripUpdate, today);
    updateSnapshot(resolver, pattern, tripUpdate, yesterday);

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

  private Result<?, UpdateError> updateSnapshot(
    TimetableSnapshot resolver,
    TripPattern pattern,
    GtfsRealtime.TripUpdate tripUpdate,
    LocalDate serviceDate
  ) {
    final Timetable scheduledTimetable = pattern.getScheduledTimetable();
    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      scheduledTimetable,
      new TripUpdate(tripUpdate),
      timeZone,
      serviceDate,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    if (result.isSuccess()) {
      return resolver.update(
        new RealTimeTripUpdate(pattern, result.successValue().tripTimes(), serviceDate)
      );
    }
    throw new RuntimeException("createUpdatedTripTimes returned an error: " + result);
  }
}
