package org.opentripplanner.updater.stoptime;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * This class starts an Paho MQTT client which opens a connection to a GTFS-RT data source.
 * A callback is registered which handles incoming GTFS-RT messages as they stream in by placing a
 * GTFS-RT decoder Runnable task in the single-threaded executor for handling.
 *
 * Usage example in the file 'router-config.json', inside the 'updaters' array:
 *
 * <pre>
 * {
 *   "id": "hsl-trip-updates",
 *   "type": "mqtt-gtfs-rt-updater",
 *   "url": "tcp://mqtt.cinfra.fi",
 *   "topic": "gtfsrt/v2/fi/hsl/tu",
 *   "feedId": "HSL",
 *   "fuzzyTripMatching": true
 * }
 * </pre>
 *
 */
public class MqttGtfsRealtimeUpdater implements GraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(MqttGtfsRealtimeUpdater.class);

    private GraphUpdaterManager updaterManager;

    private final String url;

    private final String topic;

    private final String feedId;

    private final int qos;

    private final boolean fuzzyTripMatching;

    private final String clientId = "OpenTripPlanner-" + MqttClient.generateClientId();

    private final String configRef;

    MemoryPersistence persistence = new MemoryPersistence();

    private MqttClient client;

    public MqttGtfsRealtimeUpdater(MqttGtfsRealtimeUpdaterParameters parameters) {
        this.configRef = parameters.getConfigRef();
        this.url = parameters.getUrl();
        this.topic = parameters.getTopic();
        this.feedId = parameters.getFeedId();
        this.qos = parameters.getQos();
        this.fuzzyTripMatching = parameters.getFuzzyTripMatching();
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) {
        // Only create a realtime data snapshot source if none exists already
        TimetableSnapshotSource snapshotSource =
            graph.getOrSetupTimetableSnapshotProvider(TimetableSnapshotSource::new);

        // Set properties of realtime data snapshot source
        if (fuzzyTripMatching) {
            snapshotSource.fuzzyTripMatcher = new GtfsRealtimeFuzzyTripMatcher(new RoutingService(graph));
        }
    }

    @Override
    public void run() throws Exception {
        client = new MqttClient(url, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setAutomaticReconnect(true);
        URI parsedUrl = new URI(url);
        if (parsedUrl.getUserInfo() != null) {
            String[] userinfo = parsedUrl.getUserInfo().split(":");
            connOpts.setUserName(userinfo[0]);
            connOpts.setPassword(userinfo[1].toCharArray());
        }
        client.setCallback(new Callback());

        LOG.debug("Connecting to broker: " + url);
        client.connect(connOpts);
    }

    @Override
    public void teardown() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            LOG.error("Error disconnecting", e);
        }
    }

    @Override
    public String getConfigRef() {
        return configRef;
    }

    private class Callback implements MqttCallbackExtended {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            try {
                LOG.debug("Connected");
                client.subscribe(topic, qos);
            } catch (MqttException e) {
                LOG.warn("Could not subscribe to: " + topic);
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            LOG.debug("Disconnected");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            List<GtfsRealtime.TripUpdate> updates = null;
            boolean fullDataset = true;
            try {
                // Decode message
                GtfsRealtime.FeedMessage feedMessage = GtfsRealtime.FeedMessage.PARSER.parseFrom(message.getPayload());
                List<GtfsRealtime.FeedEntity> feedEntityList = feedMessage.getEntityList();

                // Change fullDataset value if this is an incremental update
                if (feedMessage.hasHeader()
                    && feedMessage.getHeader().hasIncrementality()
                    && feedMessage.getHeader().getIncrementality()
                        .equals(GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL)) {
                    fullDataset = false;
                }

                // Create List of TripUpdates
                updates = new ArrayList<>(feedEntityList.size());
                for (GtfsRealtime.FeedEntity feedEntity : feedEntityList) {
                    if (feedEntity.hasTripUpdate()) {
                        updates.add(feedEntity.getTripUpdate());
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                LOG.error("Could not decode gtfs-rt message:", e);
            }

            if (updates != null) {
                // Handle trip updates via graph writer runnable
                updaterManager.execute(new TripUpdateGraphWriterRunnable(
                    fullDataset,
                    updates,
                    feedId
                ));
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {}
    }
}
