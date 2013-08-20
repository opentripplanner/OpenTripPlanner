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

package org.opentripplanner.routing.edgetype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.common.IterableLibrary.filter;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.request.BannedStopSet;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.CanceledTripTimes;
import org.opentripplanner.routing.trippattern.TripUpdateList;
import org.opentripplanner.routing.trippattern.Update;
import org.opentripplanner.routing.trippattern.Update.Status;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.opentripplanner.util.TestUtils;

public class TimetableTest {
    
    private static Graph graph;
    private GenericAStar aStar = new GenericAStar();
    private static GtfsContext context;
    private static Map<AgencyAndId, TableTripPattern> patternIndex;
    private static TableTripPattern pattern;
    private static Timetable timetable;
    
    @BeforeClass
    public static void setUp() throws Exception {

        context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = new Graph();

        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(graph);
        graph.putService(CalendarServiceData.class, GtfsLibrary.createCalendarServiceData(context.getDao()));
        
        patternIndex = new HashMap<AgencyAndId, TableTripPattern>();
        for (TransitStopDepart tsd : filter(graph.getVertices(), TransitStopDepart.class)) {
            for (TransitBoardAlight tba : filter(tsd.getOutgoing(), TransitBoardAlight.class)) {
                if (!tba.isBoarding())
                    continue;
                TableTripPattern pattern = tba.getPattern();
                for (Trip trip : pattern.getTrips()) {
                    patternIndex.put(trip.getId(), pattern);
                }
            }
        }
        
        pattern = patternIndex.get(new AgencyAndId("agency", "1.1"));
        timetable = pattern.scheduledTimetable;
    }
    
    @Test
    public void testUpdate() {
        TripUpdateList tripUpdateList;
        AgencyAndId trip_1_1_id = new AgencyAndId("agency", "1.1");
        int trip_1_1_index = timetable.getTripIndex(trip_1_1_id);
        AgencyAndId stop_a_id = new AgencyAndId("agency", "A");
        AgencyAndId stop_b_id = new AgencyAndId("agency", "B");
        AgencyAndId stop_c_id = new AgencyAndId("agency", "C");
        
        @SuppressWarnings("deprecation")
        Vertex stop_a = graph.getVertex("agency_A");
        @SuppressWarnings("deprecation")
        Vertex stop_c = graph.getVertex("agency_C");
        RoutingRequest options = new RoutingRequest();

        ShortestPathTree spt;
        GraphPath path;

        // non-existing trip
        tripUpdateList = TripUpdateList.forCanceledTrip(new AgencyAndId("a", "b"), 0, new ServiceDate());
        assertFalse(timetable.update(tripUpdateList));
        
        // update trip with bad data
        tripUpdateList = TripUpdateList.forUpdatedTrip(trip_1_1_id, 0, new ServiceDate(), Collections.<Update> singletonList(
                        new Update(trip_1_1_id, stop_a_id, 0, 1200, 1200, Status.PREDICTION, 0, new ServiceDate())));
        assertFalse(timetable.update(tripUpdateList));
        
        //---
        long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 0, 0);
        long endTime;
        options.dateTime = startTime;
        
        //---
        options.setRoutingContext(graph, stop_a, stop_c);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_c, false);
        assertNotNull(path);
        endTime = startTime + 20 * 60;
        assertEquals(endTime, path.getEndTime());
        
        // update trip
        List<Update> updates = new LinkedList<Update>();
        updates.add(new Update(trip_1_1_id, stop_a_id, 0,  0*60 + 120,  0*60 + 120, Status.PREDICTION, 0, new ServiceDate()));
        updates.add(new Update(trip_1_1_id, stop_b_id, 1, 10*60 + 120, 10*60 + 120, Status.PREDICTION, 0, new ServiceDate()));
        updates.add(new Update(trip_1_1_id, stop_c_id, 2, 20*60 + 120, 20*60 + 120, Status.PREDICTION, 0, new ServiceDate()));
        tripUpdateList = TripUpdateList.forUpdatedTrip(trip_1_1_id, 0, new ServiceDate(), updates);
        assertEquals(timetable.getArrivalTime(1, trip_1_1_index), 20*60);
        assertTrue(timetable.update(tripUpdateList));
        assertEquals(timetable.getArrivalTime(1, trip_1_1_index), 20*60 + 120);

        //---
        options.setRoutingContext(graph, stop_a, stop_c);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_c, false);
        assertNotNull(path);
        endTime = startTime + 20 * 60 + 120;
        assertEquals(endTime, path.getEndTime());
        
        // cancel trip
        tripUpdateList = TripUpdateList.forCanceledTrip(trip_1_1_id, 0, new ServiceDate());
        assertTrue(timetable.update(tripUpdateList));
        assertEquals(CanceledTripTimes.class, timetable.getTripTimes(trip_1_1_index).getClass());
        
        //---
        options.setRoutingContext(graph, stop_a, stop_c);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_c, false);
        assertNotNull(path);
        endTime = startTime + 40 * 60;
        assertEquals(endTime, path.getEndTime());
    }
}
