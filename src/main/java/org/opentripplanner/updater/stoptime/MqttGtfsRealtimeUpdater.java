package org.opentripplanner.updater.stoptime;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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
 * Usage example ('bessersmith' name is an example) in the file 'Graph.properties':
 *
 * <pre>
 * bessersmith.type = mqtt-gtfs-rt-updater
 * bessersmith.feedId = hsl
 * bessersmith.url = ssl://mqtt.hsl.fi:443
 * bessersmith.topic = "gtfs/trip-updates/#"
 * </pre>
 *
 */
public class MqttGtfsRealtimeUpdater implements GraphUpdater {
    private static Logger LOG = LoggerFactory.getLogger(MqttGtfsRealtimeUpdater.class);

    private GraphUpdaterManager updaterManager;

    private String url;

    private String topic;

    private String feedId;

    private int qos;

    private boolean fuzzyTripMatching;

    private String clientId = "OpenTripPlanner-" + MqttClient.generateClientId();

    MemoryPersistence persistence = new MemoryPersistence();

    private GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

    private MqttClient client;

    @Override public void configure(Graph graph, JsonNode config) throws Exception {
        url = config.path("url").asText();
        topic = config.path("topic").asText();
        feedId = config.path("feedId").asText("");
        qos = config.path("qos").asInt(0);
        fuzzyTripMatching = config.path("fuzzyTripMatching").asBoolean(false);
    }

    @Override public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override public void setup(Graph setupGraph) throws Exception {
        // Only create a realtime data snapshot source if none exists already
        if (setupGraph.timetableSnapshotSource == null) {
            TimetableSnapshotSource snapshotSource = new TimetableSnapshotSource(setupGraph);
            // Add snapshot source to graph

            if (fuzzyTripMatching) {
                this.fuzzyTripMatcher = new GtfsRealtimeFuzzyTripMatcher(setupGraph.index);
                snapshotSource.fuzzyTripMatcher = fuzzyTripMatcher;
            }
            setupGraph.timetableSnapshotSource = (snapshotSource);
        }
    }

    @Override public void run() throws Exception {
        URI parsedUrl = new URI(url);
        client = new MqttClient(url, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setAutomaticReconnect(true);
        if (parsedUrl.getUserInfo() != null) {
            String[] userinfo = parsedUrl.getUserInfo().split(":");
            connOpts.setUserName(userinfo[0]);
            connOpts.setPassword(userinfo[1].toCharArray());

        }
        client.setCallback(new MqttCallbackExtended() {
            @Override public void connectComplete(boolean reconnect, String serverURI) {
                try {
                    LOG.debug("Connected");
                    client.subscribe(topic, qos);
                } catch (MqttException e) {
                    LOG.warn("Could not subscribe to: " + topic);
                    LOG.error(e.toString());
                }
            }

            @Override public void connectionLost(Throwable cause) {
                LOG.debug("Disconnected");
            }

            @Override public void messageArrived(String topic, MqttMessage message) throws Exception {
                GtfsRealtime.FeedMessage feedMessage;
                List<GtfsRealtime.FeedEntity> feedEntityList;
                List<GtfsRealtime.TripUpdate> updates = null;
                boolean fullDataset = true;
                try {
                    // Decode message
                    feedMessage = GtfsRealtime.FeedMessage.PARSER.parseFrom(message.getPayload());
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
                    TripUpdateGraphWriterRunnable runnable = new TripUpdateGraphWriterRunnable(
                        fullDataset, updates, feedId);
                    updaterManager.execute(runnable);
                }
            }

            @Override public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        LOG.debug("Connecting to broker: " + url);
        client.connect(connOpts);
    }

    @Override public void teardown() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

}
