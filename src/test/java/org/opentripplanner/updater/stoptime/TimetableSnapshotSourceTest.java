/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.stoptime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import java.io.File;
import java.util.Arrays;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableResolver;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.trippattern.TripTimes;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

public class TimetableSnapshotSourceTest {

    private static byte cancellation[];
    private static Graph graph = new Graph();
    private static GtfsContext context;
    private static ServiceDate serviceDate = new ServiceDate();

    private TimetableSnapshotSource updater;

    @BeforeClass
    public static void setUpClass() throws Exception {
        context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));

        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(graph);
        graph.index(new DefaultStreetVertexIndexFactory());

        TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.CANCELED);

        TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        cancellation = tripUpdateBuilder.build().toByteArray();
    }

    @Before
    public void setUp() {
        graph.putService(CalendarServiceData.class,
                GtfsLibrary.createCalendarServiceData(context.getDao()));
        updater = new TimetableSnapshotSource(graph);
    }

    @Test
    public void testGetSnapshot() throws InvalidProtocolBufferException {
        updater.applyTripUpdates(Arrays.asList(TripUpdate.parseFrom(cancellation)), "agency");

        TimetableResolver resolver = updater.getTimetableSnapshot();
        assertNotNull(resolver);
        assertSame(resolver, updater.getTimetableSnapshot());

        updater.applyTripUpdates(Arrays.asList(TripUpdate.parseFrom(cancellation)), "agency");
        assertSame(resolver, updater.getTimetableSnapshot());

        updater.setMaxSnapshotFrequency(-1);
        TimetableResolver newResolver = updater.getTimetableSnapshot();
        assertNotNull(newResolver);
        assertNotSame(resolver, newResolver);
    }

    @Test
    public void testHandleCanceledTrip() throws InvalidProtocolBufferException {
        AgencyAndId tripId = new AgencyAndId("agency", "1.1");
        AgencyAndId tripId2 = new AgencyAndId("agency", "1.2");
        Trip trip = graph.index.tripForId.get(tripId);
        TripPattern pattern = graph.index.patternForTrip.get(trip);
        int tripIndex = pattern.getScheduledTimetable().getTripIndex(tripId);
        int tripIndex2 = pattern.getScheduledTimetable().getTripIndex(tripId2);

        updater.applyTripUpdates(Arrays.asList(TripUpdate.parseFrom(cancellation)), "agency");

        TimetableResolver resolver = updater.getTimetableSnapshot();
        Timetable forToday = resolver.resolve(pattern, serviceDate);
        Timetable schedule = resolver.resolve(pattern, null);
        assertNotSame(forToday, schedule);
        assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
        assertSame(forToday.getTripTimes(tripIndex2), schedule.getTripTimes(tripIndex2));

        TripTimes tripTimes = forToday.getTripTimes(tripIndex);
        for (int i = 0; i < tripTimes.getNumStops(); i++) {
            assertEquals(TripTimes.UNAVAILABLE, tripTimes.getDepartureTime(i));
            assertEquals(TripTimes.UNAVAILABLE, tripTimes.getArrivalTime(i));
        }
    }

    @Test
    public void testHandleModifiedTrip() {
        AgencyAndId tripId = new AgencyAndId("agency", "1.1");
        AgencyAndId tripId2 = new AgencyAndId("agency", "1.2");
        Trip trip = graph.index.tripForId.get(tripId);
        TripPattern pattern = graph.index.patternForTrip.get(trip);
        int tripIndex = pattern.getScheduledTimetable().getTripIndex(tripId);
        int tripIndex2 = pattern.getScheduledTimetable().getTripIndex(tripId2);

        TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(
               TripDescriptor.ScheduleRelationship.SCHEDULED);

        TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();

        stopTimeUpdateBuilder.setScheduleRelationship(
                StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopSequence(2);

        StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
        StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();

        arrivalBuilder.setDelay(1);
        departureBuilder.setDelay(1);

        TripUpdate tripUpdate = tripUpdateBuilder.build();

        updater.applyTripUpdates(Arrays.asList(tripUpdate), "agency");

        TimetableResolver resolver = updater.getTimetableSnapshot();
        Timetable forToday = resolver.resolve(pattern, serviceDate);
        Timetable schedule = resolver.resolve(pattern, null);
        assertNotSame(forToday, schedule);
        assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
        assertSame(forToday.getTripTimes(tripIndex2), schedule.getTripTimes(tripIndex2));
        assertEquals(1, forToday.getTripTimes(tripIndex).getArrivalDelay(1));
        assertEquals(1, forToday.getTripTimes(tripIndex).getDepartureDelay(1));
    }

    @Test
    public void testPurgeExpiredData() throws InvalidProtocolBufferException {
        AgencyAndId tripId = new AgencyAndId("agency", "1.1");
        ServiceDate previously = serviceDate.previous().previous(); // Just to be safe...
        Trip trip = graph.index.tripForId.get(tripId);
        TripPattern pattern = graph.index.patternForTrip.get(trip);

        updater.setMaxSnapshotFrequency(0);
        updater.setPurgeExpiredData(false);

        updater.applyTripUpdates(Arrays.asList(TripUpdate.parseFrom(cancellation)), "agency");
        TimetableResolver resolverA = updater.getTimetableSnapshot();

        updater.setPurgeExpiredData(true);

        TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.CANCELED);
        tripDescriptorBuilder.setStartDate(previously.getAsString());

        TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        TripUpdate tripUpdate = tripUpdateBuilder.build();

        updater.applyTripUpdates(Arrays.asList(tripUpdate), "agency");
        TimetableResolver resolverB = updater.getTimetableSnapshot();

        assertNotSame(resolverA, resolverB);

        assertSame   (resolverA.resolve(pattern, null ), resolverB.resolve(pattern, null ));
        assertSame   (resolverA.resolve(pattern, serviceDate),
                resolverB.resolve(pattern, serviceDate));
        assertNotSame(resolverA.resolve(pattern, null ), resolverA.resolve(pattern, serviceDate));
        assertSame   (resolverB.resolve(pattern, null ), resolverB.resolve(pattern, previously));

        // TODO: write test for added trips
    }
}
