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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.opentripplanner.routing.graph.Graph;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;

public class TripUpdateGraphWriterRunnableTest {
    @Test
    public void testTripUpdateGraphWriterRunnable() {
        final boolean fullDataset = false;
        final String agencyId = "Agency ID";
        final List<TripUpdate> updates =
                Collections.singletonList(TripUpdate.newBuilder().buildPartial());
        final TripUpdateGraphWriterRunnable tripUpdateGraphWriterRunnable =
                new TripUpdateGraphWriterRunnable(fullDataset, updates, agencyId);

        Graph graph = mock(Graph.class);
        TimetableSnapshotSource timetableSnapshotSource = mock(TimetableSnapshotSource.class);

        graph.timetableSnapshotSource = timetableSnapshotSource;

        tripUpdateGraphWriterRunnable.run(graph);

        verify(timetableSnapshotSource).applyTripUpdates(graph, fullDataset, updates, agencyId);
    }
}
