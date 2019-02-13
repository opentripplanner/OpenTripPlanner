package org.opentripplanner.routing.core;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

/**
 * Test ignoring realtime updates.
 */
public class TestIgnoreRealtimeUpdates extends TestCase {
    
    public void testIgnoreRealtimeUpdates() throws Exception {
        // Create routing request
        RoutingRequest options = new RoutingRequest();
        
        // Check that realtime updates are not ignored
        assertFalse(options.ignoreRealtimeUpdates);
        
        // Create (very simple) new graph
        Graph graph = new Graph();
        
        Stop stop1 = new Stop();
        stop1.setId(new FeedScopedId("agency", "stop1"));
        Stop stop2 = new Stop();
        stop2.setId(new FeedScopedId("agency", "stop2"));
        
        Vertex from = new TransitStop(graph, stop1);
        Vertex to = new TransitStop(graph, stop2);
        
        // Create dummy TimetableSnapshot
        TimetableSnapshot snapshot = new TimetableSnapshot();
        
        // Mock TimetableSnapshotSource to return dummy TimetableSnapshot
        TimetableSnapshotSource source = mock(TimetableSnapshotSource.class);
        when(source.getTimetableSnapshot()).thenReturn(snapshot);
        graph.timetableSnapshotSource = (source);
        
        // Create routing context
        RoutingContext rctx = new RoutingContext(options, graph, from, to);
        
        // Check that the resolver is set as timetable snapshot
        assertNotNull(rctx.timetableSnapshot);
        
        // Now set routing request to ignore realtime updates
        options.ignoreRealtimeUpdates = (true);
        
        // Check that realtime updates are ignored
        assertTrue(options.ignoreRealtimeUpdates);
        
        // Create new routing context
        rctx = new RoutingContext(options, graph, from, to);
        
        // Check that the timetable snapshot is null in the new routing context
        assertNull(rctx.timetableSnapshot);
    }
}
