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

package org.opentripplanner.routing.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.opentripplanner.util.TestUtils;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship;

/**
 * This is a singleton class to hold graph data between test runs, since loading it is slow.
 */
class Context {
    public String feedId;

    public Graph graph;

    public AStar aStar;

    private static Context instance = null;

    public static Context getInstance() throws IOException {
        if (instance == null) {
            instance = new Context();
        }
        return instance;
    }

    public Context() throws IOException {
        // Create a star search
        aStar = new AStar();

        // Create graph
        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = spy(new Graph());
        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(graph);
        graph.index(new DefaultStreetVertexIndexFactory());
        graph.putService(CalendarServiceData.class,
                GtfsLibrary.createCalendarServiceData(context.getDao()));

        feedId = context.getFeedId().getId();
        // Add simple transfer to make transfer possible between N-K and F-H
        createSimpleTransfer(feedId + ":K", feedId + ":F", 100);

        // Add simple transfer to make transfer possible between O-P and U-V
        createSimpleTransfer(feedId + ":P", feedId + ":U", 100);

        // Add simple transfer to make transfer possible between U-V and I-J
        createSimpleTransfer(feedId + ":V", feedId + ":I", 100);

        // Create dummy TimetableSnapshot
        TimetableSnapshot snapshot = new TimetableSnapshot();

        // Mock TimetableSnapshotSource to return dummy TimetableSnapshot
        TimetableSnapshotSource timetableSnapshotSource = mock(TimetableSnapshotSource.class);

        when(timetableSnapshotSource.getTimetableSnapshot()).thenReturn(snapshot);

        graph.timetableSnapshotSource = (timetableSnapshotSource);
    }

    /**
     * Create simple transfer edge between two vertices given their labels
     * @param from is label of from vertex
     * @param to is label of to vertex
     * @param distance is distance of transfer
     */
    private void createSimpleTransfer(String from, String to, int distance) {
        TransitStop fromv = ((TransitStop) graph.getVertex(from));
        TransitStop tov = ((TransitStop) graph.getVertex(to));
        new SimpleTransfer(fromv, tov, distance, null);
    }
}

/**
 * Test transfers, mostly stop-to-stop transfers.
 */
public class TestTransfers extends TestCase {
    private Graph graph;

    private AStar aStar;

    private String feedId;

    public void setUp() throws Exception {
        // Get graph, a star & feed id from singleton class
        graph = Context.getInstance().graph;
        aStar = Context.getInstance().aStar;
        feedId = Context.getInstance().feedId;
    }

    /**
     * Plan journey without optimization and return list of states and edges
     * @param options are options to use for planning the journey
     * @return ordered list of states and edges in the journey
     */
    private GraphPath planJourney(RoutingRequest options) {
        return planJourney(options, false);
    }

    /**
     * Plan journey and return list of states and edges
     * @param options are options to use for planning the journey
     * @param optimize is true when optimization should be used
     * @return ordered list of states and edges in the journey
     */
    private GraphPath planJourney(RoutingRequest options, boolean optimize) {
        // Calculate route and convert to path
        ShortestPathTree spt = aStar.getShortestPathTree(options);
        GraphPath path = spt.getPath(options.rctx.target, optimize);

        // Return list of states and edges in the journey
        return path;
    }

    private List<Trip> extractTrips(GraphPath path) {
        // Get all trips in order
        List<Trip> trips = new ArrayList<Trip>();
        if (path != null) {
            for (State s : path.states) {
                if (s.getBackMode() != null && s.getBackMode().isTransit()) {
                    Trip trip = s.getBackTrip();
                    if (trip != null && !trips.contains(trip)) {
                        trips.add(trip);
                    }
                }
            }
        }

        // Return trips
        return trips;
    }

    /**
     * Apply an update to a table trip pattern and check whether the update was applied correctly
     */
    private void applyUpdateToTripPattern(TripPattern pattern, String tripId, String stopId,
            int stopSeq, int arrive, int depart, ScheduleRelationship scheduleRelationship,
            int timestamp, ServiceDate serviceDate) throws ParseException {
        TimetableSnapshot snapshot = graph.timetableSnapshotSource.getTimetableSnapshot();
        Timetable timetable = snapshot.resolve(pattern, serviceDate);
        TimeZone timeZone = new SimpleTimeZone(-7, "PST");
        long today = serviceDate.getAsDate(timeZone).getTime() / 1000;
        TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId(tripId);

        StopTimeEvent.Builder departStopTimeEventBuilder = StopTimeEvent.newBuilder();
        StopTimeEvent.Builder arriveStopTimeEventBuilder = StopTimeEvent.newBuilder();

        departStopTimeEventBuilder.setTime(today + depart);
        arriveStopTimeEventBuilder.setTime(today + arrive);

        StopTimeUpdate.Builder stopTimeUpdateBuilder = StopTimeUpdate.newBuilder();

        stopTimeUpdateBuilder.setStopSequence(stopSeq);
        stopTimeUpdateBuilder.setDeparture(departStopTimeEventBuilder);
        stopTimeUpdateBuilder.setArrival(arriveStopTimeEventBuilder);
        stopTimeUpdateBuilder.setScheduleRelationship(scheduleRelationship);

        TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        tripUpdateBuilder.addStopTimeUpdate(0, stopTimeUpdateBuilder);

        TripUpdate tripUpdate = tripUpdateBuilder.build();

        TripTimes updatedTripTimes = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate); 
        assertNotNull(updatedTripTimes);
        int tripIndex = timetable.getTripIndex(tripId);
        assertTrue(tripIndex != -1);
        timetable.setTripTimes(tripIndex, updatedTripTimes);
    }

    public void testStopToStopTransfer() throws Exception {
        // Replace the transfer table with an empty table
        TransferTable table = new TransferTable();
        when(graph.getTransferTable()).thenReturn(table);

        // Compute a normal path between two stops
        Vertex origin = graph.getVertex(feedId + ":N");
        Vertex destination = graph.getVertex(feedId + ":H");

        // Set options like time and routing context
        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 7, 11, 11, 11, 0);
        options.setRoutingContext(graph, origin, destination);

        // Plan journey
        GraphPath path;
        List<Trip> trips;
        path = planJourney(options);
        trips = extractTrips(path);
        // Validate result
        assertEquals("8.1", trips.get(0).getId().getId());
        assertEquals("4.2", trips.get(1).getId().getId());

        // Add transfer to table, transfer time was 27600 seconds
        Stop stopK = new Stop();
        stopK.setId(new AgencyAndId(feedId, "K"));
        Stop stopF = new Stop();
        stopF.setId(new AgencyAndId(feedId, "F"));
        table.addTransferTime(stopK, stopF, null, null, null, null, 27601);

        // Plan journey
        path = planJourney(options);
        trips = extractTrips(path);
        // Check whether a later second trip was taken
        assertEquals("8.1", trips.get(0).getId().getId());
        assertEquals("4.3", trips.get(1).getId().getId());

        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }

    public void testStopToStopTransferInReverse() throws Exception {
        // Replace the transfer table with an empty table
        TransferTable table = new TransferTable();
        when(graph.getTransferTable()).thenReturn(table);

        // Compute a normal path between two stops
        Vertex origin = graph.getVertex(feedId + ":N");
        Vertex destination = graph.getVertex(feedId + ":H");

        // Set options like time and routing context
        RoutingRequest options = new RoutingRequest();
        options.setArriveBy(true);
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 7, 12, 1, 0, 0);
        options.setRoutingContext(graph, origin, destination);

        // Plan journey
        GraphPath path;
        List<Trip> trips;
        path = planJourney(options, true);
        trips = extractTrips(path);
        // Validate result
        assertEquals("8.1", trips.get(0).getId().getId());
        assertEquals("4.2", trips.get(1).getId().getId());

        // Add transfer to table, transfer time was 27600 seconds
        Stop stopK = new Stop();
        stopK.setId(new AgencyAndId(feedId, "K"));
        Stop stopF = new Stop();
        stopF.setId(new AgencyAndId(feedId, "F"));
        table.addTransferTime(stopK, stopF, null, null, null, null, 27601);

        // Plan journey
        path = planJourney(options, true);
        trips = extractTrips(path);
        // Check whether a later second trip was taken
        assertEquals("8.1", trips.get(0).getId().getId());
        assertEquals("4.3", trips.get(1).getId().getId());

        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }

    public void testStopToStopTransferWithFrequency() throws Exception {
        // Replace the transfer table with an empty table
        TransferTable table = new TransferTable();
        when(graph.getTransferTable()).thenReturn(table);

        // Compute a normal path between two stops
        Vertex origin = graph.getVertex(feedId + ":O");
        Vertex destination = graph.getVertex(feedId + ":V");

        // Set options like time and routing context
        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 7, 11, 13, 11, 0);
        options.setRoutingContext(graph, origin, destination);

        // Plan journey
        GraphPath path;
        List<Trip> trips;
        path = planJourney(options);
        trips = extractTrips(path);
        // Validate result
        assertEquals("10.5", trips.get(0).getId().getId());
        assertEquals("15.1", trips.get(1).getId().getId());
        // Find state with FrequencyBoard back edge and save time of that state
        long time = -1;
        for (State s : path.states) {
            if (s.getBackEdge() instanceof TransitBoardAlight && ((TransitBoardAlight)s.getBackEdge()).boarding)  {
                time = s.getTimeSeconds(); // find the final board edge, don't break
            }
        }
        assertTrue(time >= 0);

        // Add transfer to table such that the next trip will be chosen
        // (there are 3600 seconds between trips), transfer time was 75 seconds
        Stop stopP = new Stop();
        stopP.setId(new AgencyAndId(feedId, "P"));
        Stop stopU = new Stop();
        stopU.setId(new AgencyAndId(feedId, "U"));
        table.addTransferTime(stopP, stopU, null, null, null, null, 3675);

        // Plan journey
        path = planJourney(options);
        trips = extractTrips(path);
        // Check whether a later second trip was taken
        assertEquals("10.5", trips.get(0).getId().getId());
        assertEquals("15.1", trips.get(1).getId().getId());
        // Find state with FrequencyBoard back edge and save time of that state
        long newTime = -1;
        for (State s : path.states) {
            if (s.getBackEdge() instanceof TransitBoardAlight && ((TransitBoardAlight)s.getBackEdge()).boarding)  {
                newTime = s.getTimeSeconds(); // find the final board edge, don't break
            }
        }
        assertTrue(newTime >= 0);
        assertTrue(newTime > time);
        assertEquals(3600, newTime - time);

        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }

    public void testStopToStopTransferWithFrequencyInReverse() throws Exception {
        // Replace the transfer table with an empty table
        TransferTable table = new TransferTable();
        when(graph.getTransferTable()).thenReturn(table);

        // Compute a normal path between two stops
        Vertex origin = graph.getVertex(feedId + ":U");
        Vertex destination = graph.getVertex(feedId + ":J");

        // Set options like time and routing context
        RoutingRequest options = new RoutingRequest();
        options.setArriveBy(true);
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 7, 11, 11, 11, 0);
        options.setRoutingContext(graph, origin, destination);

        // Plan journey
        GraphPath path;
        List<Trip> trips;
        path = planJourney(options);
        trips = extractTrips(path);
        // Validate result
        assertEquals("15.1", trips.get(0).getId().getId());
        assertEquals("5.1", trips.get(1).getId().getId());
        // Find state with FrequencyBoard back edge and save time of that state
        long time = -1;
        for (State s : path.states) {
            if (s.getBackEdge() instanceof TransitBoardAlight
                    && s.getBackState() != null) {
                time = s.getBackState().getTimeSeconds();
                break;
            }
        }
        assertTrue(time >= 0);

        // Add transfer to table such that the next trip will be chosen
        // (there are 3600 seconds between trips), transfer time was 75 seconds
        Stop stopV = new Stop();
        stopV.setId(new AgencyAndId(feedId, "V"));
        Stop stopI = new Stop();
        stopI.setId(new AgencyAndId(feedId, "I"));
        table.addTransferTime(stopV, stopI, null, null, null, null, 3675);

        // Plan journey
        path = planJourney(options);
        trips = extractTrips(path);
        // Check whether a later second trip was taken
        assertEquals("15.1", trips.get(0).getId().getId());
        assertEquals("5.1", trips.get(1).getId().getId());
        // Find state with FrequencyBoard back edge and save time of that state
        long newTime = -1;
        for (State s : path.states) {
            if (s.getBackEdge() instanceof TransitBoardAlight
                    && s.getBackState() != null) {
                newTime = s.getBackState().getTimeSeconds();
                break;
            }
        }
        assertTrue(newTime >= 0);
        assertTrue(newTime < time);
        assertEquals(3600, time - newTime);

        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }

    public void testForbiddenStopToStopTransfer() throws Exception {
        // Replace the transfer table with an empty table
        TransferTable table = new TransferTable();
        when(graph.getTransferTable()).thenReturn(table);

        // Compute a normal path between two stops
        Vertex origin = graph.getVertex(feedId + ":N");
        Vertex destination = graph.getVertex(feedId + ":H");

        // Set options like time and routing context
        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 7, 11, 11, 11, 0);
        options.setRoutingContext(graph, origin, destination);

        // Plan journey
        GraphPath path;
        List<Trip> trips;
        path = planJourney(options);
        trips = extractTrips(path);
        // Validate result
        assertEquals("8.1", trips.get(0).getId().getId());
        assertEquals("4.2", trips.get(1).getId().getId());

        // Add forbidden transfer to table
        Stop stopK = new Stop();
        stopK.setId(new AgencyAndId(feedId, "K"));
        Stop stopF = new Stop();
        stopF.setId(new AgencyAndId(feedId, "F"));
        table.addTransferTime(stopK, stopF, null, null, null, null,
                StopTransfer.FORBIDDEN_TRANSFER);

        // Plan journey
        path = planJourney(options);
        trips = extractTrips(path);
        // Check that no trip was returned
        assertEquals(0, trips.size());

        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }

    public void testForbiddenStopToStopTransferWithFrequencyInReverse() throws Exception {
        // Replace the transfer table with an empty table
        TransferTable table = new TransferTable();
        when(graph.getTransferTable()).thenReturn(table);

        // Compute a normal path between two stops
        Vertex origin = graph.getVertex(feedId + ":U");
        Vertex destination = graph.getVertex(feedId + ":J");

        // Set options like time and routing context
        RoutingRequest options = new RoutingRequest();
        options.setArriveBy(true);
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 7, 11, 11, 11, 0);
        options.setRoutingContext(graph, origin, destination);

        // Plan journey
        GraphPath path;
        List<Trip> trips;
        path = planJourney(options);
        trips = extractTrips(path);
        // Validate result
        assertEquals("15.1", trips.get(0).getId().getId());
        assertEquals("5.1", trips.get(1).getId().getId());

        // Add forbidden transfer to table
        Stop stopV = new Stop();
        stopV.setId(new AgencyAndId(feedId, "V"));
        Stop stopI = new Stop();
        stopI.setId(new AgencyAndId(feedId, "I"));
        table.addTransferTime(stopV, stopI, null, null, null, null,
                StopTransfer.FORBIDDEN_TRANSFER);

        // Plan journey
        path = planJourney(options);
        trips = extractTrips(path);
        // Check that no trip was returned
        assertEquals(0, trips.size());

        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }

    public void testTimedStopToStopTransfer() throws Exception {
        ServiceDate serviceDate = new ServiceDate(2009, 07, 11);

        // Replace the transfer table with an empty table
        TransferTable table = new TransferTable();
        when(graph.getTransferTable()).thenReturn(table);

        // Compute a normal path between two stops
        Vertex origin = graph.getVertex(feedId + ":N");
        Vertex destination = graph.getVertex(feedId + ":H");

        // Set options like time and routing context
        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("America/New_York", 2009, 7, 11, 11, 11, 0);
        options.setRoutingContext(graph, origin, destination);

        // Plan journey
        GraphPath path;
        List<Trip> trips;
        path = planJourney(options);
        trips = extractTrips(path);
        // Validate result
        assertEquals("8.1", trips.get(0).getId().getId());
        assertEquals("4.2", trips.get(1).getId().getId());

        // Add timed transfer to table
        Stop stopK = new Stop();
        stopK.setId(new AgencyAndId(feedId, "K"));
        Stop stopF = new Stop();
        stopF.setId(new AgencyAndId(feedId, "F"));
        table.addTransferTime(stopK, stopF, null, null, null, null, StopTransfer.TIMED_TRANSFER);
        // Don't forget to also add a TimedTransferEdge
        Vertex fromVertex = graph.getVertex(feedId + ":K_arrive");
        Vertex toVertex = graph.getVertex(feedId + ":F_depart");
        TimedTransferEdge timedTransferEdge = new TimedTransferEdge(fromVertex, toVertex);

        // Plan journey
        path = planJourney(options);
        trips = extractTrips(path);
        // Check whether the trips are still the same
        assertEquals("8.1", trips.get(0).getId().getId());
        assertEquals("4.2", trips.get(1).getId().getId());

        // Now apply a real-time update: let the to-trip be early by 27600 seconds,
        // resulting in a transfer time of 0 seconds
        Trip trip = graph.index.tripForId.get(new AgencyAndId("agency", "4.2"));
        TripPattern pattern = graph.index.patternForTrip.get(trip);
        applyUpdateToTripPattern(pattern, "4.2", "F", 1, 55200, 55200,
                ScheduleRelationship.SCHEDULED, 0, serviceDate);

        // Plan journey
        path = planJourney(options);
        trips = extractTrips(path);
        // Check whether the trips are still the same
        assertEquals("8.1", trips.get(0).getId().getId());
        assertEquals("4.2", trips.get(1).getId().getId());

        // Now apply a real-time update: let the to-trip be early by 27601 seconds,
        // resulting in a transfer time of -1 seconds
        applyUpdateToTripPattern(pattern, "4.2", "F", 1, 55199, 55199,
                ScheduleRelationship.SCHEDULED, 0, serviceDate);

        // Plan journey
        path = planJourney(options);
        trips = extractTrips(path);
        // Check whether a later second trip was taken
        assertEquals("8.1", trips.get(0).getId().getId());
        assertEquals("4.3", trips.get(1).getId().getId());

        // "Revert" the real-time update
        applyUpdateToTripPattern(pattern, "4.2", "F", 1, 82800, 82800,
                ScheduleRelationship.SCHEDULED, 0, serviceDate);
        // Remove the timed transfer from the graph
        graph.removeEdge(timedTransferEdge);
        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }
}
