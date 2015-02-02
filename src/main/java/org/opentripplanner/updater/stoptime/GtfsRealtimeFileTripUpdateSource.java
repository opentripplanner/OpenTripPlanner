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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;

/** Reads the GTFS-RT from a local file. */
public class GtfsRealtimeFileTripUpdateSource implements TripUpdateSource, JsonConfigurable {
    private static final Logger LOG =
            LoggerFactory.getLogger(GtfsRealtimeFileTripUpdateSource.class);

    private File file;

    /**
     * Default agency id that is used for the trip ids in the TripUpdates
     */
    private String agencyId;

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        this.agencyId = config.path("defaultAgencyId").asText();
        this.file = new File(config.path("file").asText(""));
    }

    @Override
    public List<TripUpdate> getUpdates() {
        FeedMessage feedMessage = null;
        List<FeedEntity> feedEntityList = null;
        List<TripUpdate> updates = null;
        try {
            InputStream is = new FileInputStream(file);
            if (is != null) {
                feedMessage = FeedMessage.PARSER.parseFrom(is);
                feedEntityList = feedMessage.getEntityList();
                updates = new ArrayList<TripUpdate>(feedEntityList.size());
                for (FeedEntity feedEntity : feedEntityList) {
                    updates.add(feedEntity.getTripUpdate());
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse gtfs-rt feed at " + file + ":", e);
        }
        return updates;
    }

    public String toString() {
        return "GtfsRealtimeFileTripUpdateSource(" + file + ")";
    }

	@Override
	public String getAgencyId() {
		return this.agencyId;
	}
}
