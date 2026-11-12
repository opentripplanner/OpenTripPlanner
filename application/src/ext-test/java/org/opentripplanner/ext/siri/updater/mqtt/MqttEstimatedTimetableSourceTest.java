package org.opentripplanner.ext.siri.updater.mqtt;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MqttEstimatedTimetableSourceTest {

  private static final String TOPIC = "siri/et";
  private static final byte[] PAYLOAD = "<Siri/>".getBytes(StandardCharsets.UTF_8);

  private final MqttEstimatedTimetableSource source = new MqttEstimatedTimetableSource(
    new MqttSiriETUpdaterParameters(
      "ref",
      "F",
      "localhost",
      1883,
      null,
      null,
      TOPIC,
      0,
      false,
      1,
      Duration.ofSeconds(1),
      Duration.ofSeconds(1)
    )
  );

  @AfterEach
  void tearDown() {
    source.teardown();
  }

  @Test
  void emptyPayloadIsIgnored() {
    source.onMessage(Mqtt5Publish.builder().topic(TOPIC).build());
    source.onMessage(
      Mqtt5Publish.builder()
        .topic(TOPIC)
        .payload(new byte[0])
        .build()
    );
    source.onMessage(Mqtt5Publish.builder().topic(TOPIC).retain(true).build());

    assertThat(source.liveMessageQueue).isEmpty();
    assertThat(source.primingMessageQueue).isEmpty();
  }

  @Test
  void liveMessageIsQueued() {
    source.onMessage(Mqtt5Publish.builder().topic(TOPIC).payload(PAYLOAD).build());

    assertThat(source.liveMessageQueue).hasSize(1);
    assertArrayEquals(PAYLOAD, source.liveMessageQueue.peek());
    assertThat(source.primingMessageQueue).isEmpty();
  }

  @Test
  void retainedMessageIsQueuedForPriming() {
    source.onMessage(Mqtt5Publish.builder().topic(TOPIC).payload(PAYLOAD).retain(true).build());

    assertThat(source.primingMessageQueue).hasSize(1);
    assertArrayEquals(PAYLOAD, source.primingMessageQueue.peek());
    assertThat(source.liveMessageQueue).isEmpty();
  }
}
