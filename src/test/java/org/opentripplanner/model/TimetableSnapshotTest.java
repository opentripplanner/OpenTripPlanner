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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.model.basic.FeedScopedId;

public class TimetableSnapshotTest {

  private static final TimeZone timeZone = TimeZone.getTimeZone("GMT");
  private static Map<FeedScopedId, TripPattern> patternIndex;
  static String feedId;

  @BeforeAll
  public static void setUp() throws Exception {
    Graph graph = ConstantsForTests.buildGtfsGraph(ConstantsForTests.FAKE_GTFS);

    feedId = graph.getFeedIds().iterator().next();

    patternIndex = new HashMap<>();
    for (TripPattern tripPattern : graph.tripPatternForId.values()) {
      tripPattern
        .scheduledTripsAsStream()
        .forEach(trip -> patternIndex.put(trip.getId(), tripPattern));
    }
  }

  @Test
  public void testCompare() {
    Timetable orig = new Timetable(null);
    Timetable a = new Timetable(orig, new ServiceDate().previous());
    Timetable b = new Timetable(orig, new ServiceDate());
    assertTrue(new TimetableSnapshot.SortedTimetableComparator().compare(a, b) < 0);
  }

  @Test
  public void testResolve() {
    ServiceDate today = new ServiceDate();
    ServiceDate yesterday = today.previous();
    ServiceDate tomorrow = today.next();
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
        ServiceDate today = new ServiceDate();
        ServiceDate yesterday = today.previous();
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
        ServiceDate today = new ServiceDate();
        ServiceDate yesterday = today.previous();
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
        assertTrue(updateResolver(resolver, pattern, tripUpdate, today));
        snapshot = resolver.commit();
        assertEquals(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
        assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

        // add a new timetable for today, don't commit, and everything should not match
        assertTrue(updateResolver(resolver, pattern, tripUpdate, today));
        assertNotSame(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
        assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

        // add a new timetable for today, on another day, and things should still not match
        assertTrue(updateResolver(resolver, pattern, tripUpdate, yesterday));
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
    ServiceDate today = new ServiceDate();
    ServiceDate yesterday = today.previous();
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

  private boolean updateResolver(
    TimetableSnapshot resolver,
    TripPattern pattern,
    TripUpdate tripUpdate,
    ServiceDate serviceDate
  ) {
    TripTimesPatch tripTimesPatch = pattern
      .getScheduledTimetable()
      .createUpdatedTripTimes(tripUpdate, timeZone, serviceDate);
    TripTimes updatedTripTimes = tripTimesPatch.getTripTimes();
    return resolver.update(pattern, updatedTripTimes, serviceDate);
  }
}
