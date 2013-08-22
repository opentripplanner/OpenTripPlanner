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

package org.opentripplanner.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.stoptime.GTFSZMQUpdateStreamer;
import org.opentripplanner.updater.stoptime.GtfsRealtimeHttpUpdateStreamer;
import org.opentripplanner.updater.stoptime.KV8ZMQUpdateStreamer;
import org.opentripplanner.updater.stoptime.StoptimeUpdater;
import org.opentripplanner.updater.stoptime.UpdateStreamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configure a graph by creating stop time updater.
 * 
 * Usage example ('rt' name is an example):
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
public class StopTimeUpdateConfigurator implements PreferencesConfigurable {

    private static final long DEFAULT_UPDATE_FREQ_SEC = 60;

    private static Logger LOG = LoggerFactory.getLogger(StopTimeUpdateConfigurator.class);

    private static Map<String, Class<? extends UpdateStreamer>> updateStreamers;

    static {
        // List of all possible update streamer types with corresponding 'type' key.
        updateStreamers = new HashMap<String, Class<? extends UpdateStreamer>>();
        updateStreamers.put("gtfs-http", GtfsRealtimeHttpUpdateStreamer.class);
        updateStreamers.put("gtfs-zmq", GTFSZMQUpdateStreamer.class);
        updateStreamers.put("kv8-zmq", KV8ZMQUpdateStreamer.class);
    }

    @Override
    public void configure(Graph graph, Preferences preferences) throws Exception {
        String sourceType = preferences.get("sourceType", null);
        Class<? extends UpdateStreamer> clazz = updateStreamers.get(sourceType);
        if (clazz == null) {
            LOG.error("Unknown update streamer source type: " + sourceType);
            return;
        }
        UpdateStreamer updateStreamer = clazz.newInstance();
        if (updateStreamer instanceof PreferencesConfigurable) {
            // If the source itself is a configurable, let's configure it.
            ((PreferencesConfigurable) updateStreamer).configure(graph, preferences);
        }
        StoptimeUpdater updater = new StoptimeUpdater(graph);
        updater.setUpdateStreamer(updateStreamer);
        // Configure the updater itself
        int logFrequency = preferences.getInt("logFrequency", -1);
        if (logFrequency >= 0)
            updater.setLogFrequency(logFrequency);
        int maxSnapshotFrequency = preferences.getInt("maxSnapshotFrequencyMs", -1);
        if (maxSnapshotFrequency >= 0)
            updater.setMaxSnapshotFrequency(maxSnapshotFrequency);
        updater.setPurgeExpiredData(preferences.getBoolean("purgeExpiredData", true));
        long frequencySec = preferences.getLong("frequencySec", DEFAULT_UPDATE_FREQ_SEC);
        LOG.info("Creating stop time updater running every {} seconds : {}", frequencySec,
                updateStreamer);
        GraphUpdaterManager updaterManager = graph.getUpdaterManager();
        updaterManager.addUpdater(updater, frequencySec * 1000);
    }
}
