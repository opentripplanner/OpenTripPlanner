package org.opentripplanner.updater.trip.gtfs.updater.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.trip.gtfs.interpolation.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.interpolation.ForwardsDelayPropagationType;

class MqttGtfsRealtimeUpdaterTest {

  private static final String TOPIC = "gtfsrt/tu";

  private final AtomicInteger graphWrites = new AtomicInteger();
  private MqttGtfsRealtimeUpdater updater;

  @BeforeEach
  void setUp() {
    updater = new MqttGtfsRealtimeUpdater(
      new MqttGtfsRealtimeUpdaterParameters(
        "ref",
        "F",
        "tcp://localhost",
        TOPIC,
        0,
        false,
        ForwardsDelayPropagationType.DEFAULT,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      ),
      null
    );
    updater.setup(_ -> {
      graphWrites.incrementAndGet();
      return CompletableFuture.completedFuture(null);
    });
  }

  @Test
  void emptyPayloadIsIgnored() {
    updater.onMessage(Mqtt5Publish.builder().topic(TOPIC).build());
    updater.onMessage(
      Mqtt5Publish.builder()
        .topic(TOPIC)
        .payload(new byte[0])
        .build()
    );

    assertEquals(0, graphWrites.get());
  }

  @Test
  void validMessageWithoutEntitiesIsProcessed() {
    var feedMessage = FeedMessage.newBuilder()
      .setHeader(FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0"))
      .build();

    updater.onMessage(
      Mqtt5Publish.builder().topic(TOPIC).payload(feedMessage.toByteArray()).build()
    );

    assertEquals(1, graphWrites.get());
  }
}
