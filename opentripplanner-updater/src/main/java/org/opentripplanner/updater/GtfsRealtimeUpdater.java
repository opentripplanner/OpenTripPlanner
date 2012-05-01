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

import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public class GtfsRealtimeUpdater implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(GtfsRealtimeUpdater.class);

    private String url;

    private String defaultAgencyId;

    private PatchService patchService;

    private long earlyStart;

    private UpdateHandler updateHandler = null;

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setDefaultAgencyId(String defaultAgencyId) {
        this.defaultAgencyId = defaultAgencyId;
    }

    public void run() {
        try {
            InputStream data = HttpUtils.getData(url);
            if (data == null) {
                throw new RuntimeException("Failed to get data from url " + url);
            }
            if(updateHandler == null) {
                updateHandler = new UpdateHandler();
            }
            updateHandler.setEarlyStart(earlyStart);
            updateHandler.setDefaultAgencyId(defaultAgencyId);
            updateHandler.setPatchService(getPatchService());

            FeedMessage feed = GtfsRealtime.FeedMessage.parseFrom(data);
            updateHandler.update(feed);
        } catch (IOException e) {
            log.error("Eror reading gtfs-realtime feed from " + url, e);
        }
    }

    @Autowired
    public void setPatchService(PatchService patchService) {
        this.patchService = patchService;
    }

    public PatchService getPatchService() {
        return patchService;
    }

    public long getEarlyStart() {
        return earlyStart;
    }

    public void setEarlyStart(long earlyStart) {
        this.earlyStart = earlyStart;
    }
    public String toString() {
        return "GtfsRealtimeUpdater(" + url + ")";
    }

}
