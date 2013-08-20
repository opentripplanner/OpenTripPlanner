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
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public class GTFSZMQUpdateStreamer extends GtfsRealtimeAbstractUpdateStreamer {
    private static final Logger LOG = LoggerFactory.getLogger(GTFSZMQUpdateStreamer.class);

    private static final File file = new File("/var/otp/data/nl/gtfs-rt.protobuf");

    @Override
    protected FeedMessage getFeedMessage() {
        FeedMessage feed = null;
        try {
            InputStream is = new FileInputStream(file);
            feed = GtfsRealtime.FeedMessage.PARSER.parseFrom(is);
        } catch (IOException e) {
            LOG.warn("Failed to parse gtfs-rt feed at " + file + ":", e);
        }
        return feed;
    }

    public String toString() {
        return "GTFSZMQUpdateStreamer(" + file + ")";
    }
}
