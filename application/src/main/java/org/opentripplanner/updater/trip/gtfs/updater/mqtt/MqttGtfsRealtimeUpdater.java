package org.opentripplanner.updater.trip.gtfs.updater.mqtt;

import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.gtfs.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.ForwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.GtfsRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.trip.gtfs.updater.TripUpdateGraphWriterRunnable;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class starts a hive MQTT client which opens a connection to a GTFS-RT data source. A
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
  private final ForwardsDelayPropagationType forwardsDelayPropagationType;
  private final BackwardsDelayPropagationType backwardsDelayPropagationType;
  private final String configRef;
  private final GtfsRealTimeTripUpdateAdapter adapter;
  private final Consumer<UpdateResult> recordMetrics;
  private WriteToGraphCallback saveResultOnGraph;

  private final boolean fuzzyTripMatching;

  private Mqtt5AsyncClient client;

  public MqttGtfsRealtimeUpdater(
    MqttGtfsRealtimeUpdaterParameters parameters,
    GtfsRealTimeTripUpdateAdapter adapter
  ) {
    this.configRef = parameters.configRef();
    this.url = parameters.url();
    this.topic = parameters.topic();
    this.feedId = parameters.feedId();
    this.qos = parameters.qos();
    this.forwardsDelayPropagationType = parameters.forwardsDelayPropagationType();
    this.backwardsDelayPropagationType = parameters.backwardsDelayPropagationType();
    this.adapter = adapter;
    // Set properties of realtime data snapshot source
    this.fuzzyTripMatching = parameters.fuzzyTripMatching();
    this.recordMetrics = TripUpdateMetrics.streaming(parameters);
    LOG.info("Creating streaming GTFS-RT TripUpdate updater subscribing to MQTT broker at {}", url);
  }

  @Override
  public void setup(WriteToGraphCallback writeToGraphCallback) {
    this.saveResultOnGraph = writeToGraphCallback;
  }

  @Override
  public void run() throws Exception {
    client = connectAndSubscribeToClient();
  }

  private Mqtt5AsyncClient connectAndSubscribeToClient() throws URISyntaxException {
    URI parsedUrl = new URI(url);
    Mqtt5SimpleAuth auth = createAuthFromUrl(parsedUrl);

    Mqtt5ClientBuilder mqtt5ClientBuilder = Mqtt5Client.builder()
      .identifier("OpenTripPlanner-" + UUID.randomUUID())
      .serverHost(parsedUrl.getHost())
      .simpleAuth(auth)
      .automaticReconnectWithDefaultConfig()
      .addConnectedListener(ctx -> onConnect())
      .addDisconnectedListener(this::onDisconnect);

    if (parsedUrl.getPort() != -1) {
      mqtt5ClientBuilder = mqtt5ClientBuilder.serverPort(parsedUrl.getPort());
    }

    Mqtt5AsyncClient asyncClient = mqtt5ClientBuilder.buildAsync();

    asyncClient.connectWith().keepAlive(30).cleanStart(true).send().join();

    asyncClient
      .subscribeWith()
      .topicFilter(topic)
      .qos(Optional.ofNullable(MqttQos.fromCode(qos)).orElse(MqttQos.AT_MOST_ONCE))
      .callback(this::onMessage)
      .send()
      .join();

    return asyncClient;
  }

  private Mqtt5SimpleAuth createAuthFromUrl(URI parsedUrl) {
    if (parsedUrl.getUserInfo() != null) {
      String[] userinfo = parsedUrl.getUserInfo().split(":");
      return Mqtt5SimpleAuth.builder()
        .username(userinfo[0])
        .password(userinfo[1].getBytes(StandardCharsets.UTF_8))
        .build();
    }
    return null;
  }

  private void onDisconnect(MqttClientDisconnectedContext ctx) {
    LOG.info("Disconnected client from MQTT broker: {}", url, ctx.getCause());
  }

  private void onConnect() {
    LOG.info("Connected client to MQTT broker: {} with qos: {}", url, qos);
  }

  @Override
  public void teardown() {
    client.disconnect();
  }

  @Override
  public String getConfigRef() {
    return configRef;
  }

  private void onMessage(Mqtt5Publish message) {
    List<GtfsRealtime.TripUpdate> updates = null;
    UpdateIncrementality updateIncrementality = FULL_DATASET;
    try {
      // Decode message
      GtfsRealtime.FeedMessage feedMessage = GtfsRealtime.FeedMessage.parseFrom(
        message.getPayloadAsBytes()
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
          forwardsDelayPropagationType,
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
  public String toString() {
    return ToStringBuilder.of(MqttGtfsRealtimeUpdater.class)
      .addStr("url", url)
      .addStr("topic", topic)
      .addStr("feedId", feedId)
      .toString();
  }
}
