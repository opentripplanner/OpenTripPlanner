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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
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
        stop1.setId(new AgencyAndId("agency", "stop1"));
        Stop stop2 = new Stop();
        stop2.setId(new AgencyAndId("agency", "stop2"));
        
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
