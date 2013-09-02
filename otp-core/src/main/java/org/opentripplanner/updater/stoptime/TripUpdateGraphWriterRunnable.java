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

import lombok.AllArgsConstructor;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripUpdateList;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class TripUpdateGraphWriterRunnable implements GraphWriterRunnable {

    private static Logger LOG = LoggerFactory.getLogger(TripUpdateGraphWriterRunnable.class);

    /**
     * The list with updates to apply to the graph
     */
    private List<TripUpdateList> updates;

    @Override
    public void run(Graph graph) {
        // Apply updates to graph using realtime snapshot source
        TimetableSnapshotSource snapshotSource = graph.getTimetableSnapshotSource();
        if (snapshotSource != null) {
            snapshotSource.applyTripUpdateLists(updates);
        } else {
            LOG.error("Could not find realtime data snapshot source in graph."
                    + " The following updates are not applied: {}", updates);
        }
    }
}
