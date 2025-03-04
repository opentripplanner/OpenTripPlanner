package org.opentripplanner.updater.trip.gtfs.updater.mqtt;

import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.gtfs.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.GtfsRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.trip.gtfs.updater.TripUpdateGraphWriterRunnable;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class starts an Paho MQTT client which opens a connection to a GTFS-RT data source. A
 * callback is registered which handles incoming GTFS-RT messages as they stream in by placing a
 * GTFS-RT decoder Runnable task in the single-threaded executor for handling.
 * <p>
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
 */
public class MqttGtfsRealtimeUpdater implements GraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(MqttGtfsRealtimeUpdater.class);
  private final String url;
  private final String topic;
  private final String feedId;
  private final int qos;
  private final BackwardsDelayPropagationType backwardsDelayPropagationType;
  private final String clientId = "OpenTripPlanner-" + MqttClient.generateClientId();
  private final String configRef;
  private final MemoryPersistence persistence = new MemoryPersistence();
  private final GtfsRealTimeTripUpdateAdapter adapter;
  private final Consumer<UpdateResult> recordMetrics;
  private WriteToGraphCallback saveResultOnGraph;

  private final boolean fuzzyTripMatching;

  private MqttClient client;

  public MqttGtfsRealtimeUpdater(
    MqttGtfsRealtimeUpdaterParameters parameters,
    GtfsRealTimeTripUpdateAdapter adapter
  ) {
    this.configRef = parameters.configRef();
    this.url = parameters.url();
    this.topic = parameters.getTopic();
    this.feedId = parameters.feedId();
    this.qos = parameters.getQos();
    this.backwardsDelayPropagationType = parameters.getBackwardsDelayPropagationType();
    this.adapter = adapter;
    // Set properties of realtime data snapshot source
    this.fuzzyTripMatching = parameters.getFuzzyTripMatching();
    this.recordMetrics = TripUpdateMetrics.streaming(parameters);
    LOG.info("Creating streaming GTFS-RT TripUpdate updater subscribing to MQTT broker at {}", url);
  }

  @Override
  public void setup(WriteToGraphCallback writeToGraphCallback) {
    this.saveResultOnGraph = writeToGraphCallback;
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

    LOG.debug("Connecting to broker: {}", url);
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
        LOG.warn("Could not subscribe to: {}", topic);
      }
    }

    @Override
    public void connectionLost(Throwable cause) {
      LOG.debug("Disconnected");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
      List<GtfsRealtime.TripUpdate> updates = null;
      UpdateIncrementality updateIncrementality = FULL_DATASET;
      try {
        // Decode message
        GtfsRealtime.FeedMessage feedMessage = GtfsRealtime.FeedMessage.parseFrom(
          message.getPayload()
        );
        List<GtfsRealtime.FeedEntity> feedEntityList = feedMessage.getEntityList();

        // Change fullDataset value if this is an incremental update
        if (
          feedMessage.hasHeader() &&
          feedMessage.getHeader().hasIncrementality() &&
          feedMessage
            .getHeader()
            .getIncrementality()
            .equals(GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL)
        ) {
          updateIncrementality = DIFFERENTIAL;
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
        saveResultOnGraph.execute(
          new TripUpdateGraphWriterRunnable(
            adapter,
            fuzzyTripMatching,
            backwardsDelayPropagationType,
            updateIncrementality,
            updates,
            feedId,
            recordMetrics
          )
        );
      }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(MqttGtfsRealtimeUpdater.class)
      .addStr("url", url)
      .addStr("topic", topic)
      .addStr("feedId", feedId)
      .toString();
  }
}
