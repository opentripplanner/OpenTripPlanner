package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.BackwardsDelayPropagationType;

public class TimetableSnapshotTest {

  private static final ZoneId timeZone = ZoneIds.GMT;
  private static Map<FeedScopedId, TripPattern> patternIndex;
  static String feedId;

  @BeforeAll
  public static void setUp() throws Exception {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.SIMPLE_GTFS);
    TransitModel transitModel = model.transitModel();

    feedId = transitModel.getFeedIds().iterator().next();

    patternIndex = new HashMap<>();
    for (TripPattern tripPattern : transitModel.getAllTripPatterns()) {
      tripPattern
        .scheduledTripsAsStream()
        .forEach(trip -> patternIndex.put(trip.getId(), tripPattern));
    }
  }

  @Test
  public void testCompare() {
    Timetable orig = new Timetable(null);
    Timetable a = new Timetable(orig, LocalDate.now(timeZone).minusDays(1));
    Timetable b = new Timetable(orig, LocalDate.now(timeZone));
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
    Assertions.assertThrows(
      ConcurrentModificationException.class,
      () -> {
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
        stopTimeUpdateBuilder.setDeparture(
          TripUpdate.StopTimeEvent.newBuilder().setDelay(5).build()
        );

        TripUpdate tripUpdate = tripUpdateBuilder.build();

        // new timetable for today
        updateResolver(resolver, pattern, tripUpdate, today);
        Timetable updatedNow = resolver.resolve(pattern, today);
        assertNotSame(origNow, updatedNow);

        // reuse timetable for today
        updateResolver(resolver, pattern, tripUpdate, today);
        assertEquals(updatedNow, resolver.resolve(pattern, today));

        // create new timetable for tomorrow
        updateResolver(resolver, pattern, tripUpdate, yesterday);
        assertNotSame(origNow, resolver.resolve(pattern, yesterday));
        assertNotSame(updatedNow, resolver.resolve(pattern, yesterday));

        // exception if we try to modify a snapshot
        TimetableSnapshot snapshot = resolver.commit();
        updateResolver(snapshot, pattern, tripUpdate, yesterday);
      }
    );
  }

  @Test
  public void testCommit() {
    Assertions.assertThrows(
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
      return resolver.update(pattern, result.successValue().getTripTimes(), serviceDate);
    }
    throw new RuntimeException("createUpdatedTripTimes returned an error: " + result);
  }
}
