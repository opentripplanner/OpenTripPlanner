package org.opentripplanner.model;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsContextBuilder;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TimetableSnapshotTest {
    private static Map<FeedScopedId, TripPattern> patternIndex;
    private static TimeZone timeZone = TimeZone.getTimeZone("GMT");

    @BeforeClass
    public static void setUp() throws Exception {
        Graph graph = new Graph();
        GtfsContext context = GtfsContextBuilder
                .contextBuilder(ConstantsForTests.FAKE_GTFS)
                .withIssueStoreAndDeduplicator(graph)
                .build();

        GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(context);
        factory.run(graph);
        graph.putService(CalendarServiceData.class,
                context.getCalendarServiceData());

        patternIndex = new HashMap<>();
        for (TripPattern tripPattern : graph.tripPatternForId.values()) {
            for (Trip trip : tripPattern.getTrips()) {
                patternIndex.put(trip.getId(), tripPattern);
            }
        }
    }

    @Test
    public void testCompare() {
        Timetable orig = new Timetable(null);
        Timetable a = new Timetable(orig, new ServiceDate().previous());
        Timetable b = new Timetable(orig, new ServiceDate());
        assertEquals(-1, (new TimetableSnapshot.SortedTimetableComparator()).compare(a, b));
    }
    
    private boolean updateResolver(TimetableSnapshot resolver, TripPattern pattern, TripUpdate tripUpdate, String feedId, ServiceDate serviceDate) {
        TripTimes updatedTripTimes = pattern.scheduledTimetable.createUpdatedTripTimes(tripUpdate,
                timeZone, serviceDate);
        return resolver.update(pattern, updatedTripTimes, serviceDate);
    }

    @Test
    public void testResolve() {
        ServiceDate today = new ServiceDate();
        ServiceDate yesterday = today.previous();
        ServiceDate tomorrow = today.next();
        TripPattern pattern = patternIndex.get(new FeedScopedId("agency", "1.1"));
        TimetableSnapshot resolver = new TimetableSnapshot();

        Timetable scheduled = resolver.resolve(pattern, today);
        assertEquals(scheduled, resolver.resolve(pattern, null));

        TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.CANCELED);

        TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        TripUpdate tripUpdate = tripUpdateBuilder.build();

        // add a new timetable for today
        updateResolver(resolver, pattern, tripUpdate, "agency", today);
        Timetable forNow = resolver.resolve(pattern, today);
        assertEquals(scheduled, resolver.resolve(pattern, yesterday));
        assertNotSame(scheduled, forNow);
        assertEquals(scheduled, resolver.resolve(pattern, tomorrow));
        assertEquals(scheduled, resolver.resolve(pattern, null));

        // add a new timetable for yesterday
        updateResolver(resolver, pattern, tripUpdate, "agency", yesterday);
        Timetable forYesterday = resolver.resolve(pattern, yesterday);
        assertNotSame(scheduled, forYesterday);
        assertNotSame(scheduled, forNow);
        assertEquals(scheduled, resolver.resolve(pattern, tomorrow));
        assertEquals(scheduled, resolver.resolve(pattern, null));
    }

    @Test(expected=ConcurrentModificationException.class)
    public void testUpdate() {
        ServiceDate today = new ServiceDate();
        ServiceDate yesterday = today.previous();
        TripPattern pattern = patternIndex.get(new FeedScopedId("agency", "1.1"));

        TimetableSnapshot resolver = new TimetableSnapshot();
        Timetable origNow = resolver.resolve(pattern, today);

        TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.CANCELED);

        TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        TripUpdate tripUpdate = tripUpdateBuilder.build();

        // new timetable for today
        updateResolver(resolver, pattern, tripUpdate, "agency", today);
        Timetable updatedNow = resolver.resolve(pattern, today);
        assertNotSame(origNow, updatedNow);

        // reuse timetable for today
        updateResolver(resolver, pattern, tripUpdate, "agency", today);
        assertEquals(updatedNow, resolver.resolve(pattern, today));

        // create new timetable for tomorrow
        updateResolver(resolver, pattern, tripUpdate, "agency", yesterday);
        assertNotSame(origNow, resolver.resolve(pattern, yesterday));
        assertNotSame(updatedNow, resolver.resolve(pattern, yesterday));

        // exception if we try to modify a snapshot
        TimetableSnapshot snapshot = resolver.commit();
        updateResolver(snapshot, pattern, tripUpdate, "agency", yesterday);
    }

    @Test(expected=ConcurrentModificationException.class)
    public void testCommit() {
        ServiceDate today = new ServiceDate();
        ServiceDate yesterday = today.previous();
        TripPattern pattern = patternIndex.get(new FeedScopedId("agency", "1.1"));

        TimetableSnapshot resolver = new TimetableSnapshot();

        // only return a new snapshot if there are changes
        TimetableSnapshot snapshot = resolver.commit();
        assertNull(snapshot);

        TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.CANCELED);

        TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        TripUpdate tripUpdate = tripUpdateBuilder.build();

        // add a new timetable for today, commit, and everything should match
        assertTrue(updateResolver(resolver, pattern, tripUpdate, "agency", today));
        snapshot = resolver.commit();
        assertEquals(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
        assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

        // add a new timetable for today, don't commit, and everything should not match
        assertTrue(updateResolver(resolver, pattern, tripUpdate, "agency", today));
        assertNotSame(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
        assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

        // add a new timetable for today, on another day, and things should still not match
        assertTrue(updateResolver(resolver, pattern, tripUpdate, "agency", yesterday));
        assertNotSame(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

        // commit, and things should match
        snapshot = resolver.commit();
        assertEquals(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
        assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

        // exception if we try to commit to a snapshot
        snapshot.commit();
    }

    @Test
    public void testPurge() {
        ServiceDate today = new ServiceDate();
        ServiceDate yesterday = today.previous();
        TripPattern pattern = patternIndex.get(new FeedScopedId("agency", "1.1"));

        TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.CANCELED);

        TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        TripUpdate tripUpdate = tripUpdateBuilder.build();

        TimetableSnapshot resolver = new TimetableSnapshot();
        updateResolver(resolver, pattern, tripUpdate, "agency", today);
        updateResolver(resolver, pattern, tripUpdate, "agency", yesterday);

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
}
