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
import java.util.Collections;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.impl.transit_index.TransitIndexBuilder;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableResolver;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.trippattern.CanceledTripTimes;
import org.opentripplanner.routing.trippattern.DecayingDelayTripTimes;
import org.opentripplanner.routing.trippattern.TripUpdateList;
import org.opentripplanner.routing.trippattern.Update;

public class TimetableSnapshotSourceTest {

    private static Graph graph = new Graph();
    private static GtfsContext context;
    private static TransitIndexService transitIndexService;
    
    private TimetableSnapshotSource updater;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        context = GtfsLibrary.readGtfs(new File("../otp-core/" + ConstantsForTests.FAKE_GTFS));

        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(graph);
        
        TransitIndexBuilder builder = new TransitIndexBuilder();
        builder.setDao(context.getDao());
        builder.buildGraph(graph);
        
        transitIndexService = graph.getService(TransitIndexService.class);
    }
    
    @Before
    public void setUp() {
        updater = new TimetableSnapshotSource(graph);
        graph.putService(CalendarServiceData.class, GtfsLibrary.createCalendarServiceData(context.getDao()));
    }
    
    @Test
    public void testGetSnapshot() {
        AgencyAndId tripId = new AgencyAndId("agency", "1.1");
        ServiceDate serviceDate = new ServiceDate();
        
        TripUpdateList tripUpdateList = TripUpdateList.forCanceledTrip(tripId, 0, serviceDate);
        updater.applyTripUpdateLists(Arrays.asList(tripUpdateList));
        
        TimetableResolver resolver = updater.getTimetableSnapshot();
        assertNotNull(resolver);
        assertSame(resolver, updater.getTimetableSnapshot());
        
        tripUpdateList = TripUpdateList.forCanceledTrip(tripId, 0, serviceDate);
        updater.applyTripUpdateLists(Arrays.asList(tripUpdateList));
        assertSame(resolver, updater.getTimetableSnapshot());

        updater.setMaxSnapshotFrequency(-1);
        TimetableResolver newResolver = updater.getTimetableSnapshot();
        assertNotNull(newResolver);
        assertNotSame(resolver, newResolver);
    }
    
    @Test
    public void testHandleCanceledTrip() { 
        AgencyAndId tripId = new AgencyAndId("agency", "1.1");
        AgencyAndId tripId2 = new AgencyAndId("agency", "1.2");
        ServiceDate today = new ServiceDate();
        TableTripPattern pattern = transitIndexService.getTripPatternForTrip(tripId);
        int tripIndex = pattern.getTripIndex(tripId);
        int tripIndex2 = pattern.getTripIndex(tripId2);
        
        TripUpdateList tripUpdateList = TripUpdateList.forCanceledTrip(tripId, 0, today);
        updater.applyTripUpdateLists(Arrays.asList(tripUpdateList));
        
        TimetableResolver resolver = updater.getTimetableSnapshot();
        Timetable forToday = resolver.resolve(pattern, today);
        Timetable schedule = resolver.resolve(pattern, null);
        assertNotSame(forToday, schedule);
        assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
        assertEquals(CanceledTripTimes.class, forToday.getTripTimes(tripIndex).getClass());
        assertSame(forToday.getTripTimes(tripIndex2), schedule.getTripTimes(tripIndex2));
    }
    
    @Test
    public void testHandleModifiedTrip() { 
        AgencyAndId tripId = new AgencyAndId("agency", "1.1");
        AgencyAndId tripId2 = new AgencyAndId("agency", "1.2");
        AgencyAndId stopId = new AgencyAndId("agency", "A");
        ServiceDate today = new ServiceDate();
        TableTripPattern pattern = transitIndexService.getTripPatternForTrip(tripId);
        int tripIndex = pattern.getTripIndex(tripId);
        int tripIndex2 = pattern.getTripIndex(tripId2);
        
        Update u = new Update(tripId, stopId, 0, 0, Update.Status.PREDICTION, 0, today);
        TripUpdateList tripUpdateList = TripUpdateList.forUpdatedTrip(tripId, 0, today, Collections.singletonList(u));
        updater.applyTripUpdateLists(Arrays.asList(tripUpdateList));
        
        TimetableResolver resolver = updater.getTimetableSnapshot();
        Timetable forToday = resolver.resolve(pattern, today);
        Timetable schedule = resolver.resolve(pattern, null);
        assertNotSame(forToday, schedule);
        assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
        assertEquals(DecayingDelayTripTimes.class, forToday.getTripTimes(tripIndex).getClass());
        assertSame(forToday.getTripTimes(tripIndex2), schedule.getTripTimes(tripIndex2));
    }

    @Test
    public void testPurgeExpiredData() {
        AgencyAndId tripId = new AgencyAndId("agency", "1.1");
        ServiceDate today = new ServiceDate();
        ServiceDate previously = today.previous().previous(); // Just to be safe...
        TableTripPattern pattern = transitIndexService.getTripPatternForTrip(tripId);
        
        updater.setMaxSnapshotFrequency(0);
        updater.setPurgeExpiredData(false);
        
        TripUpdateList tripUpdateList = TripUpdateList.forCanceledTrip(tripId, 0, today);
        updater.applyTripUpdateLists(Arrays.asList(tripUpdateList));
        TimetableResolver resolverA = updater.getTimetableSnapshot();
        
        updater.setPurgeExpiredData(true);
        
        tripUpdateList = TripUpdateList.forCanceledTrip(tripId, 0, previously);
        updater.applyTripUpdateLists(Arrays.asList(tripUpdateList));
        TimetableResolver resolverB = updater.getTimetableSnapshot();
        
        assertNotSame(resolverA, resolverB);
        
        assertSame   (resolverA.resolve(pattern, null ), resolverB.resolve(pattern, null ));
        assertSame   (resolverA.resolve(pattern, today), resolverB.resolve(pattern, today));
        assertNotSame(resolverA.resolve(pattern, null ), resolverA.resolve(pattern, today));
        assertSame   (resolverB.resolve(pattern, null ), resolverB.resolve(pattern, previously));
        
        // TODO: write test for added trips
    }
}
