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

package org.opentripplanner.updater;

import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.Preferences;

import lombok.Setter;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.PatchServiceImpl;
import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.transit.realtime.GtfsRealtime;

public class GtfsRealtimeUpdater implements GraphUpdaterRunnable, GraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeUpdater.class);

    private static final long DEFAULT_UPDATE_FREQ_SEC = 60;

    private Long lastTimestamp = Long.MIN_VALUE;

    @Setter
    private String url;

    @Setter
    private String defaultAgencyId;

    private PatchService patchService;

    @Setter
    private long earlyStart;

    private UpdateHandler updateHandler = null;

    @Override
    public void setup() {
        if (updateHandler == null) {
            updateHandler = new UpdateHandler();
        }
        updateHandler.setEarlyStart(earlyStart);
        updateHandler.setDefaultAgencyId(defaultAgencyId);
        updateHandler.setPatchService(patchService);
    }

    @Override
    public void run() {
        try {
            InputStream data = HttpUtils.getData(url);
            if (data == null) {
                throw new RuntimeException("Failed to get data from url " + url);
            }
            
            GtfsRealtime.FeedMessage feed = GtfsRealtime.FeedMessage.PARSER.parseFrom(data);
            
            long feedTimestamp = feed.getHeader().getTimestamp();
            if(feedTimestamp <= lastTimestamp) {
                LOG.info("Ignoring feed with an old timestamp.");
                return;
            }
        
            updateHandler.update(feed);
        
            lastTimestamp = feedTimestamp;
        } catch (IOException e) {
            LOG.error("Eror reading gtfs-realtime feed from " + url, e);
        }
    }

    @Override
    public void teardown() {
    }

    @Autowired
    public void setPatchService(PatchService patchService) {
        this.patchService = patchService;
    }

    public String toString() {
        return "GtfsRealtimeUpdater(" + url + ")";
    }

    /**
     * Configure GTFS real-time alert updater
     * 
     * Usage example ('myalert' name is an example):
     * 
     * <pre>
     * myalert.type = real-time-alerts
     * myalert.frequencySec = 60
     * myalert.url = http://host.tld/path
     * myalert.earlyStartSec = 3600
     * myalert.defaultAgencyId = TA
     * </pre>
     * 
     */
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
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        // TODO Auto-generated method stub
        
    }
}
