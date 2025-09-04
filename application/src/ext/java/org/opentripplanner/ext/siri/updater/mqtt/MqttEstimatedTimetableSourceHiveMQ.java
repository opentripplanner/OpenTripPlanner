package org.opentripplanner.ext.siri.updater.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import jakarta.xml.bind.JAXBException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import javax.xml.stream.XMLStreamException;
import org.entur.siri21.util.SiriXml;
import org.opentripplanner.updater.trip.siri.updater.AsyncEstimatedTimetableSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.ServiceDelivery;
import uk.org.siri.siri21.Siri;

public class MqttEstimatedTimetableSourceHiveMQ implements AsyncEstimatedTimetableSource {

  private static final Logger LOG = LoggerFactory.getLogger(MqttEstimatedTimetableSourceHiveMQ.class);

  private final MqttSiriETUpdaterParameters parameters;

  private Mqtt5AsyncClient client;
  private Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer;

  private final BlockingQueue<byte[]> messageQueue = new LinkedBlockingQueue<>();
  private final ExecutorService primingExecutor;
  private final ExecutorService liveExecutor;

  private volatile boolean primed = false;

  private volatile Instant timestampOfLastHistoricDelivery;
  private volatile Instant timestampOfLastDelivery;

  private Instant connectedAt;
  private final AtomicInteger receivedMessageCounter = new AtomicInteger();
  private final AtomicInteger processedMessageCounter = new AtomicInteger();
  private final AtomicLong receivedMessageSize = new AtomicLong();
  private final AtomicLong processedMessageSize = new AtomicLong();

  public MqttEstimatedTimetableSourceHiveMQ(MqttSiriETUpdaterParameters parameters) {
    this.parameters = parameters;
    this.primingExecutor = Executors.newFixedThreadPool(parameters.numberOfPrimingWorkers());
    this.liveExecutor = Executors.newSingleThreadExecutor();
  }

  @Override
  public void start(Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer) {
    this.serviceDeliveryConsumer = serviceDeliveryConsumer;

    client = connectAndSubscribeToClient();
    connectedAt = Instant.now();

    timestampOfLastHistoricDelivery = Instant.now().plusSeconds(30);
    List<CompletableFuture<Void>> primingFutures = new ArrayList<>();

    for (int i = 0; i < parameters.numberOfPrimingWorkers(); i++) {
      CompletableFuture<Void> f = CompletableFuture.runAsync(
        new PrimeRunner(i),
        primingExecutor
      );
      primingFutures.add(f);
    }
    LOG.info("Started {} priming workers", parameters.numberOfPrimingWorkers());

    // Wait for priming workers to finish
    CompletableFuture<Void> allPriming = CompletableFuture.allOf(
      primingFutures.toArray(new CompletableFuture[0])
    );

    // when all are done, switch to live
    allPriming.thenRunAsync(() -> {
      logMessages(processedMessageSize.get(),  processedMessageCounter.get(), "Processed in total");
      LOG.info("All priming workers done after {} seconds, starting live worker",
        connectedAt.until(Instant.now(), ChronoUnit.SECONDS));
      liveExecutor.submit(new LiveRunner());
      primingExecutor.shutdown();
    }).exceptionally(ex -> {
      LOG.error("Priming failed", ex);
      return null;
    });

  }

  private Mqtt5AsyncClient connectAndSubscribeToClient() {
    Mqtt5SimpleAuth auth;
    if (parameters.user() == null || parameters.user().isBlank()
      || parameters.password() == null || parameters.password().isBlank()) {
      auth = null;
    } else {
      auth = Mqtt5SimpleAuth.builder()
        .username(parameters.user())
        .password(parameters.password().getBytes(StandardCharsets.UTF_8))
        .build();
    }
    Mqtt5AsyncClient client = Mqtt5Client.builder()
      .identifier("OpenTripPlanner-" + UUID.randomUUID())
      .serverHost(parameters.host())
      .serverPort(parameters.port())
      .simpleAuth(auth)
      .automaticReconnectWithDefaultConfig()
      .addConnectedListener(ctx -> onConnect())
      .addDisconnectedListener(this::onDisconnect)
      .buildAsync();

    client.connectWith()
      .keepAlive(30)
      .cleanStart(true)
      .send()
      .join();

    client.subscribeWith()
      .topicFilter(parameters.topic())
      .qos(Optional.ofNullable(MqttQos.fromCode(parameters.qos())).orElse(MqttQos.AT_MOST_ONCE))
      .callback(this::onMessage)
      .send()
      .join();

    return client;
  }

  private void onDisconnect(MqttClientDisconnectedContext ctx) {
    LOG.info("Disconnected client from MQTT broker: {}",
      parameters.url(), ctx.getCause());
  }

  private void onConnect() {
    LOG.info("Connected client to MQTT broker: {} with qos: {}",
      parameters.url(), parameters.qos());
  }

  @Override
  public boolean isPrimed() {
    return true;
  }

  @Override
  public void teardown() {
    client.disconnect();
  }

  private Optional<ServiceDelivery> serviceDelivery(byte[] payload) {
    Siri siri;
    try {
      siri = SiriXml.parseXml(new String(payload, StandardCharsets.UTF_8));
    } catch (XMLStreamException | JAXBException e) {
      LOG.warn("Failed to parse Siri XML", e);
      return Optional.empty();
    }
    return Optional.ofNullable(siri.getServiceDelivery());
  }

  private void onMessage(Mqtt5Publish message) {
    timestampOfLastDelivery = Instant.now();
    int numberOfMessages = receivedMessageCounter.incrementAndGet();
    long sizeBytes = receivedMessageSize.addAndGet(message.getPayloadAsBytes().length);
    boolean offer = messageQueue.offer(message.getPayloadAsBytes());
    if (!offer) {
      LOG.warn("Failed to offer to message queue");
    }
    if (!primed && numberOfMessages % 1000 == 0) {
      logMessages(sizeBytes, numberOfMessages, "Received");
      LOG.info("Queue size: {}", messageQueue.size());
    }
  }

  private void logMessages(long totalMessageSize, int messageCount, String prefix) {
    double sizeMb = totalMessageSize / 1024. / 1024.;
    double meanMessageSizeKB = totalMessageSize / (double) messageCount / 1024.;
    long totalMillis = connectedAt.until(Instant.now(), ChronoUnit.MILLIS);
    double messageRate = (double) messageCount / totalMillis * 1000;
    LOG.info("{} {} messages. Total size: {} MB, mean message size: {} kB, mean message rate: {} per second.",
      prefix,
      messageCount,
      String.format("%.2f", sizeMb),
      String.format("%.2f", meanMessageSizeKB),
      String.format("%.2f", messageRate)
    );
  }

  private class PrimeRunner implements Runnable {

    public static final Duration MAX_PRIMING_IDLE = Duration.ofSeconds(5);
    private static final Duration THRESHOLD_HISTORIC_DATA = Duration.ofMinutes(5);
    private static final int SECONDS_SINCE_LAST_HISTORIC_DELIVERY = 3;

    private final int workerId;

    private PrimeRunner(int workerId) {
      this.workerId = workerId;
    }

    @Override
    public void run() {
      try {
        while (!primed && !Thread.currentThread().isInterrupted()) {
          byte[] payload = messageQueue.take();
          var optionalServiceDelivery = serviceDelivery(payload);
          if (optionalServiceDelivery.isEmpty()) {
            return;
          }
          var serviceDelivery = optionalServiceDelivery.get();
          serviceDeliveryConsumer.apply(serviceDelivery);
          if (
            serviceDelivery.getResponseTimestamp()
              .plus(THRESHOLD_HISTORIC_DATA)
              .isBefore(ZonedDateTime.now())
          ) {
            timestampOfLastHistoricDelivery = Instant.now();
          }
          int messageCount = processedMessageCounter.incrementAndGet();
          long totalMessageSize = processedMessageSize.addAndGet(payload.length);

          if (messageCount % 1000 == 0) {
            logMessages(totalMessageSize, messageCount, "Processed");
          }

          if (
            timestampOfLastHistoricDelivery.plusSeconds(SECONDS_SINCE_LAST_HISTORIC_DELIVERY)
              .isBefore(Instant.now())
              || timestampOfLastDelivery.plus(MAX_PRIMING_IDLE).isBefore(Instant.now())
          ) {
            primed = true;
          }
        }
        LOG.info("Priming worker {} done",  workerId);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class LiveRunner implements Runnable {
    @Override
    public void run() {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          byte[] payload = messageQueue.take();
          serviceDelivery(payload).ifPresent(serviceDeliveryConsumer::apply);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
