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

import java.util.List;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;

public class TripUpdateGraphWriterRunnable implements GraphWriterRunnable {
    private static Logger LOG = LoggerFactory.getLogger(TripUpdateGraphWriterRunnable.class);

    /**
     * True iff the list with updates represent all updates that are active right now, i.e. all
     * previous updates should be disregarded
     */
    private final boolean fullDataset;
    
    /**
     * The list with updates to apply to the graph
     */
    private final List<TripUpdate> updates;

    private final String feedId;

    public TripUpdateGraphWriterRunnable(final boolean fullDataset, final List<TripUpdate> updates, final String feedId) {
        // Preconditions
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(feedId);
        
        // Set fields
        this.fullDataset = fullDataset;
        this.updates = updates;
        this.feedId = feedId;
    }

    @Override
    public void run(Graph graph) {
        // Apply updates to graph using realtime snapshot source
        TimetableSnapshotSource snapshotSource = graph.timetableSnapshotSource;
        if (snapshotSource != null) {
            snapshotSource.applyTripUpdates(graph, fullDataset, updates, feedId);
        } else {
            LOG.error("Could not find realtime data snapshot source in graph."
                    + " The following updates are not applied: {}", updates);
        }
    }
}
