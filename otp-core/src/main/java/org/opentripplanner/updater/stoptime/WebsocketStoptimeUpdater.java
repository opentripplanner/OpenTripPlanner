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
import java.util.prefs.Preferences;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripUpdateList;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.websocket.DefaultWebSocketListener;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

/**
 * This class starts an HTTP client which opens a websocket connection to a GTFS-RT data source. A
 * callback is registered which handles incoming GTFS-RT messages as they stream in by placing a
 * GTFS-RT decoder Runnable task in the single-threaded executor for handling.
 */
public class WebsocketStoptimeUpdater implements GraphUpdater {

    private static Logger LOG = LoggerFactory.getLogger(WebsocketStoptimeUpdater.class);

    private GraphUpdaterManager updaterManager;

    private String url;
    
    private String agencyId;

    @Override
    public void configure(Graph graph, Preferences preferences) throws Exception {
        url = preferences.get("url", null);
        agencyId = preferences.get("agencyId", "");
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup() {
    }

    @Override
    public void run() {
        // The AsyncHttpClient library uses Netty by default (it has a dependency on Netty).
        // It can also make use of Grizzly for the HTTP layer, but the Jersey-Grizzly integration
        // forces us to use a version of Grizzly that is too old to be compatible with the current
        // AsyncHttpClient. This would be done as follows:
        // AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().build();
        // AsyncHttpClient client = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config),
        // config);
        // Using Netty by default:
        AsyncHttpClient client = new AsyncHttpClient();
        WebSocketListener listener = new Listener();
        WebSocketUpgradeHandler handler = new WebSocketUpgradeHandler.Builder()
                .addWebSocketListener(listener).build();
        @SuppressWarnings("unused")
        WebSocket socket;
        try {
            socket = client.prepareGet(url).execute(handler).get();
            // socket.sendMessage("I CAN HAZ GTFS-RT?".getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // client.closeAsynchronously();
    }

    @Override
    public void teardown() {
    }

    private class Listener extends DefaultWebSocketListener {
        private int n_received = 0;

        private long last_log_time = System.currentTimeMillis();

        @Override
        public void onMessage(byte[] message) {
            // Log number of messages received
            n_received += 1;
            if (n_received % 100 == 0) {
                long this_log_time = System.currentTimeMillis();
                long elapsed_time = this_log_time - last_log_time;
                LOG.info("Received {} messages in {} msec, {} msgs/sec", n_received, elapsed_time,
                        n_received / (elapsed_time / 1000d));
                n_received = 0;
                last_log_time = System.currentTimeMillis();
            }

            try {
                // Decode message into TripUpdateList
                FeedMessage feed = GtfsRealtime.FeedMessage.PARSER.parseFrom(message);
                List<TripUpdateList> updates = TripUpdateList.decodeFromGtfsRealtime(feed, agencyId);
                
                LOG.info("Updates: {}", updates);
                
                // TODO handle trip updates via graph writer
            } catch (InvalidProtocolBufferException e) {
                LOG.error("Could not decode gtfs-rt message.", e);
            }
        }
    }
}
