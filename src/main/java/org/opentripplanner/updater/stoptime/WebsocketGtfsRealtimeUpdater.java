package org.opentripplanner.updater.stoptime;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.websocket.DefaultWebSocketListener;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class starts an HTTP client which opens a websocket connection to a GTFS-RT data source. A
 * callback is registered which handles incoming GTFS-RT messages as they stream in by placing a
 * GTFS-RT decoder Runnable task in the single-threaded executor for handling.
 *
 * <pre>
 * websocket.type = websocket-gtfs-rt-updater
 * websocket.defaultAgencyId = agency
 * websocket.url = ws://localhost:8088/tripUpdates
 * </pre>
 *
 */
public class WebsocketGtfsRealtimeUpdater implements GraphUpdater {
    /**
     * Number of seconds to wait before checking again whether we are still connected
     */
    private static final int CHECK_CONNECTION_PERIOD_SEC = 1;

    private static final Logger LOG = LoggerFactory.getLogger(WebsocketGtfsRealtimeUpdater.class);

    /**
     * Parent update manager. Is used to execute graph writer runnables.
     */
    private WriteToGraphCallback saveResultOnGraph;

    /**
     * Url of the websocket server
     */
    private final String url;

    /**
     * The ID for the static feed to which these TripUpdates are applied
     */
    private final String feedId;

    /**
     * The number of seconds to wait before reconnecting after a failed connection.
     */
    private final int reconnectPeriodSec;

    private final String configRef;

    public WebsocketGtfsRealtimeUpdater(WebsocketGtfsRealtimeUpdaterParameters parameters) {
        this.configRef = parameters.getConfigRef();
        this.url = parameters.getUrl();
        this.feedId = parameters.getFeedId();
        this.reconnectPeriodSec = parameters.getReconnectPeriodSec();
    }

    @Override
    public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
        this.saveResultOnGraph = saveResultOnGraph;
    }

    @Override
    public void setup(Graph graph) {
        // Only create a realtime data snapshot source if none exists already
        graph.getOrSetupTimetableSnapshotProvider(TimetableSnapshotSource::new);
    }

    @Override
    public void run() throws InterruptedException {
        // The AsyncHttpClient library uses Netty by default (it has a dependency on Netty).
        // It can also make use of Grizzly for the HTTP layer, but the Jersey-Grizzly integration
        // forces us to use a version of Grizzly that is too old to be compatible with the current
        // AsyncHttpClient. This would be done as follows:
        // AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().build();
        // AsyncHttpClient client = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config),
        // config);
        // Using Netty by default:

        while (true) {
            AsyncHttpClient client = new AsyncHttpClient();
            WebSocketListener listener = new Listener();
            WebSocketUpgradeHandler handler = new WebSocketUpgradeHandler.Builder()
                    .addWebSocketListener(listener).build();
            WebSocket socket = null;
            boolean connectionSuccessful = true;
            // Try to create a websocket connection
            try {
                socket = client.prepareGet(url).execute(handler).get();
                LOG.info("Successfully connected to {}.", url);
            } catch (ExecutionException e) {
                LOG.error("Could not connect to {}: {}", url, e.getCause().getMessage());
                connectionSuccessful = false;
            } catch (Exception e) {
                LOG.error("Unknown exception when trying to connect to {}.", url, e);
                connectionSuccessful = false;
            }

            // If connection was unsuccessful, wait some time before trying again
            if (!connectionSuccessful) {
                Thread.sleep(reconnectPeriodSec * 1000);
            }

            // Keep checking whether connection is still open
            while (true) {
                if (socket == null || !socket.isOpen()) {
                    // The connection is closed somehow, try to reconnect
                    if (connectionSuccessful) {
                        LOG.warn("Connection to {} was lost. Trying to reconnect...", url);
                    }
                    break;
                }
                Thread.sleep(CHECK_CONNECTION_PERIOD_SEC * 1000);
            }

            client.close();
        }
    }

    @Override
    public void teardown() {
    }

    /**
     * Auxiliary class to handle incoming messages via the websocket connection
     */
    private class Listener extends DefaultWebSocketListener {
        @Override
        public void onMessage(byte[] message) {
            FeedMessage feedMessage;
            List<FeedEntity> feedEntityList;
            List<TripUpdate> updates = null;
            boolean fullDataset = true;
            try {
                // Decode message
                feedMessage = FeedMessage.PARSER.parseFrom(message);
                feedEntityList = feedMessage.getEntityList();
                
                // Change fullDataset value if this is an incremental update
                if (feedMessage.hasHeader()
                        && feedMessage.getHeader().hasIncrementality()
                        && feedMessage.getHeader().getIncrementality()
                                .equals(GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL)) {
                    fullDataset = false;
                }
                
                // Create List of TripUpdates
                updates = new ArrayList<>(feedEntityList.size());
                for (FeedEntity feedEntity : feedEntityList) {
                    if (feedEntity.hasTripUpdate()) {
                        updates.add(feedEntity.getTripUpdate());
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                LOG.error("Could not decode gtfs-rt message:", e);
            }

            if (updates != null) {
                // Handle trip updates via graph writer runnable
                TripUpdateGraphWriterRunnable runnable = new TripUpdateGraphWriterRunnable(
                        fullDataset, updates, feedId
                );
                saveResultOnGraph.execute(runnable);
            }
        }
    }

    @Override
    public String getConfigRef() {
        return configRef;
    }

}
