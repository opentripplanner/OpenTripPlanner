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

import java.util.prefs.Preferences;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.PatchServiceImpl;
import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.updater.GtfsRealtimeUpdater;
import org.opentripplanner.updater.PeriodicTimerGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configure a graph by creating a GTFS real-time alert connector.
 * 
 * Usage example ('myalert' name is an example):
 * 
 * <pre>
 * myalert.type = stop-time-updater
 * myalert.frequencySec = 60
 * myalert.url = http://host.tld/path
 * myalert.earlyStartSec = 3600
 * myalert.defaultAgencyId = TA
 * </pre>
 * 
 */
public class RealTimeAlertConfigurator implements PreferencesConfigurable {

    private static final long DEFAULT_UPDATE_FREQ_SEC = 60;

    private static Logger LOG = LoggerFactory.getLogger(RealTimeAlertConfigurator.class);

    @Override
    public void configure(Graph graph, Preferences preferences) throws Exception {
        GtfsRealtimeUpdater realTimeUpdater = new GtfsRealtimeUpdater();
        PatchService patchService = new PatchServiceImpl(graph);
        realTimeUpdater.setPatchService(patchService);
        String url = preferences.get("url", null);
        if (url == null)
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        realTimeUpdater.setUrl(url);
        realTimeUpdater.setEarlyStart(preferences.getInt("earlyStartSec", 0));
        realTimeUpdater.setDefaultAgencyId(preferences.get("defaultAgencyId", null));
        long frequencySec = preferences.getLong("frequencySec", DEFAULT_UPDATE_FREQ_SEC);
        LOG.info("Creating real-time alert updater running every {} seconds : {}", frequencySec,
                url);
        PeriodicTimerGraphUpdater periodicGraphUpdater = graph
                .getService(PeriodicTimerGraphUpdater.class);
        periodicGraphUpdater.addUpdater(realTimeUpdater, frequencySec * 1000);
    }
}
