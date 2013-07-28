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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.trippattern.CanceledTripTimes;
import org.opentripplanner.routing.trippattern.DecayingDelayTripTimes;
import org.opentripplanner.routing.trippattern.TripUpdate;
import org.opentripplanner.routing.trippattern.Update;

public class StoptimeUpdaterTest {

    private static Graph graph = new Graph();
    private static GtfsContext context;
    private static TransitIndexService transitIndexService;
    
    private StoptimeUpdater updater;
    private TripUpdate tripUpdate;
    
    private static GraphService graphService = new GraphService() {
        @Override public void setLoadLevel(LoadLevel level) {}
        @Override public boolean reloadGraphs(boolean preEvict) { return false; }
        @Override public boolean registerGraph(String routerId, Graph graph) { return false; }
        @Override public boolean registerGraph(String routerId, boolean preEvict) { return false; }
        @Override public Collection<String> getRouterIds() { return null; }
        @Override public Graph getGraph(String routerId) { return graph; }
        @Override public Graph getGraph() { return graph; }
        @Override public boolean evictGraph(String routerId) { return false; }
        @Override public int evictAll() { return 0; }
    };
    
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
        updater = new StoptimeUpdater();
        updater.setGraphService(graphService);
        updater.setUpdateStreamer(new UpdateStreamer() {
            @Override
            public List<TripUpdate> getUpdates() {
                return Collections.singletonList(tripUpdate);
            }
        });
        graph.putService(CalendarServiceData.class, GtfsLibrary.createCalendarServiceData(context.getDao()));
    }
    
    @Test
    public void testGetSnapshot() {
        AgencyAndId tripId = new AgencyAndId("agency", "1.1");
        ServiceDate serviceDate = new ServiceDate();
        
        tripUpdate = TripUpdate.forCanceledTrip(tripId, 0, serviceDate);
        updater.run();
        
        TimetableResolver resolver = updater.getSnapshot();
        assertNotNull(resolver);
        assertSame(resolver, updater.getSnapshot());
        
        tripUpdate = TripUpdate.forCanceledTrip(tripId, 0, serviceDate);
        updater.run();
        assertSame(resolver, updater.getSnapshot());

        updater.setMaxSnapshotFrequency(-1);
        TimetableResolver newResolver = updater.getSnapshot();
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
        
        tripUpdate = TripUpdate.forCanceledTrip(tripId, 0, today);
        updater.run();
        
        TimetableResolver resolver = updater.getSnapshot();
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
        tripUpdate = TripUpdate.forUpdatedTrip(tripId, 0, today, Collections.singletonList(u));
        updater.run();
        
        TimetableResolver resolver = updater.getSnapshot();
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
        
        tripUpdate = TripUpdate.forCanceledTrip(tripId, 0, today);
        updater.run();
        TimetableResolver resolverA = updater.getSnapshot();
        
        updater.setPurgeExpiredData(true);
        
        tripUpdate = TripUpdate.forCanceledTrip(tripId, 0, previously);
        updater.run();
        TimetableResolver resolverB = updater.getSnapshot();
        
        assertNotSame(resolverA, resolverB);
        
        assertSame   (resolverA.resolve(pattern, null ), resolverB.resolve(pattern, null ));
        assertSame   (resolverA.resolve(pattern, today), resolverB.resolve(pattern, today));
        assertNotSame(resolverA.resolve(pattern, null ), resolverA.resolve(pattern, today));
        assertSame   (resolverB.resolve(pattern, null ), resolverB.resolve(pattern, previously));
        
        // TODO: write test for added trips
    }
}
