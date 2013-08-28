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
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

import org.opentripplanner.updater.PreferencesConfigurable;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripUpdateList;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update OTP stop time tables from some (realtime) source
 * 
 * Usage example ('rt' name is an example) in file 'Graph.properties':
 * 
 * <pre>
 * rt.type = stop-time-updater
 * rt.frequencySec = 60
 * rt.sourceType = gtfs-http
 * rt.url = http://host.tld/path
 * rt.defaultAgencyId = TA
 * </pre>
 * 
 */
public class PollingStoptimeUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(PollingStoptimeUpdater.class);
    
    /**
     * Parent update manager. Is used to execute graph writer runnables. 
     */
    private GraphUpdaterManager updaterManager;

    /**
     * Update streamer
     */
    private TripUpdateSource updateSource;
    
    /**
     * Property to set on the RealtimeDataSnapshotSource
     */
    private Integer logFrequency;
    
    /**
     * Property to set on the RealtimeDataSnapshotSource
     */
    private Integer maxSnapshotFrequency;

    /**
     * Property to set on the RealtimeDataSnapshotSource
     */
    private Boolean purgeExpiredData;

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void configurePolling(Graph graph, Preferences preferences) throws Exception {
        // Create update streamer from preferences
        String sourceType = preferences.get("sourceType", null);
        if (sourceType != null) {
            if (sourceType.equals("gtfs-http")) {
                updateSource = new GtfsRealtimeHttpTripUpdateSource();
            } else if (sourceType.equals("gtfs-zmq")) {
                updateSource = new GtfsRealtimeZmqTripUpdateSource();
            } else if (sourceType.equals("kv8-zmq")) {
                updateSource = new Kv8ZmqTripUpdateSource();
            }
        }

        // Configure update source
        if (updateSource == null) {
            throw new IllegalArgumentException("Unknown update streamer source type: " + sourceType);
        } else if (updateSource instanceof PreferencesConfigurable) {
            ((PreferencesConfigurable) updateSource).configure(graph, preferences);
        }

        // Configure updater
        int logFrequency = preferences.getInt("logFrequency", -1);
        if (logFrequency >= 0)
            this.logFrequency = logFrequency;
        int maxSnapshotFrequency = preferences.getInt("maxSnapshotFrequencyMs", -1);
        if (maxSnapshotFrequency >= 0)
            this.maxSnapshotFrequency = maxSnapshotFrequency;
        String purgeExpiredData = preferences.get("purgeExpiredData", "");
        if (!purgeExpiredData.isEmpty()) {
            this.purgeExpiredData = preferences.getBoolean("purgeExpiredData", true);
        }

        LOG.info("Creating stop time updater running every {} seconds : {}", getFrequencySec(), updateSource);
    }

    @Override
    public void setup() throws InterruptedException, ExecutionException {
        // Create a realtime data snapshot source and wait for runnable to be executed
        updaterManager.executeBlocking(new GraphWriterRunnable() {
            @Override
            public void run(Graph graph) {
                // Only create a realtime data snapshot source if none exists already
                TimetableSnapshotSource snapshotSource = graph.getTimetableSnapshotSource(); 
                if (snapshotSource == null) {
                    snapshotSource = new TimetableSnapshotSource(graph);
                    // Add snapshot source to graph
                    graph.setTimetableSnapshotSource(snapshotSource);
                }
                
                // Set properties of realtime data snapshot source
                if (logFrequency != null) {
                    snapshotSource.setLogFrequency(logFrequency);
                }
                if (maxSnapshotFrequency != null) {
                    snapshotSource.setMaxSnapshotFrequency(maxSnapshotFrequency);
                }
                if (purgeExpiredData != null) {
                    snapshotSource.setPurgeExpiredData(purgeExpiredData);
                }
            }
        });
    }

    /**
     * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates, and
     * applies those updates to the graph.
     */
    @Override
    public void runPolling() throws Exception {
        // Get update lists from update source
        List<TripUpdateList> updates = updateSource.getUpdates();

        // Handle trip updates via graph writer runnable
        TripUpdateGraphWriterRunnable runnable = new TripUpdateGraphWriterRunnable(updates);
        updaterManager.execute(runnable);
    }

    @Override
    public void teardown() {
    }

    public String toString() {
        String s = (updateSource == null) ? "NONE" : updateSource.toString();
        return "Streaming stoptime updater with update source = " + s;
    }

}
