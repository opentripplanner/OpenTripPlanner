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

package org.opentripplanner.updater.carspeed;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.carspeed.CarSpeedSnapshotSource;
import org.opentripplanner.routing.carspeed.CarSpeedSnapshot.StreetEdgeCarSpeedProvider;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Dynamic car speed updater.
 * 
 * @author laurent
 */
public class CarSpeedPollingUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(CarSpeedPollingUpdater.class);

    private GraphUpdaterManager updaterManager;

    private CarSpeedDataSource source;

    public CarSpeedPollingUpdater() {
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        // Set source from preferences
        String sourceType = config.path("sourceType").asText();
        source = null;
        if (sourceType != null) {
            if (sourceType.equals("exclusion-zone")) {
                source = new ExclusionZoneCarSpeedDataSource();
            }
        }

        if (source == null) {
            throw new IllegalArgumentException("Unknown car speed source type: " + sourceType);
        } else if (source instanceof JsonConfigurable) {
            // Configure updater
            ((JsonConfigurable) source).configure(graph, config);
        }

        if (graph.carSpeedSnapshotSource == null) {
            graph.carSpeedSnapshotSource = new CarSpeedSnapshotSource();
        }

        LOG.info("Creating car speed updater running every {} seconds : {}", frequencySec, source);
    }

    @Override
    public void setup() throws InterruptedException, ExecutionException {
    }

    @Override
    protected void runPolling() throws Exception {
        LOG.debug("Updating car speeds from " + source);
        List<T2<StreetEdge, StreetEdgeCarSpeedProvider>> entries = source.getUpdatedEntries();
        if (entries == null || entries.isEmpty()) {
            LOG.debug("No updates");
            return;
        }
        /*
         * TODO Would not it be much simpler to make
         * CarSpeedSnapshotSource::updateCarSpeedProvider() synchronized?
         */
        CarSpeedGraphWriterRunnable graphWriterRunnable = new CarSpeedGraphWriterRunnable(entries);
        updaterManager.execute(graphWriterRunnable);
    }

    @Override
    public void teardown() {
    }

    private class CarSpeedGraphWriterRunnable implements GraphWriterRunnable {

        private List<T2<StreetEdge, StreetEdgeCarSpeedProvider>> entries;

        private CarSpeedGraphWriterRunnable(List<T2<StreetEdge, StreetEdgeCarSpeedProvider>> entries) {
            this.entries = entries;
        }

        @Override
        public void run(Graph graph) {
            for (T2<StreetEdge, StreetEdgeCarSpeedProvider> entry : entries) {
                graph.carSpeedSnapshotSource.updateCarSpeedProvider(entry.first, entry.second);
            }
            graph.carSpeedSnapshotSource.commit();
        }
    }
}
