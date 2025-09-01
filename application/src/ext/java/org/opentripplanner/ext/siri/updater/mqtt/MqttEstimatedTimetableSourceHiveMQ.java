package org.opentripplanner.ext.siri.updater.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
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
  private static final int PRIMING_THREADS = 1;
  private static final int NUM_CLIENTS = 1;

  private final MqttSiriETUpdaterParameters parameters;

  private final List<Mqtt5AsyncClient> clients = new ArrayList<>();
  private Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer;

  private final BlockingQueue<byte[]> messageQueue = new LinkedBlockingQueue<>();
  private final ExecutorService primingExecutor;
  private final ExecutorService liveExecutor;

  private volatile boolean primed = false;

  private volatile Instant timestampOfLastHistoricDelivery;
  private Instant connectedAt;
  private final AtomicInteger primedMessageCounter = new AtomicInteger();
  private final AtomicInteger retainedMessageCounter = new AtomicInteger();
  private final AtomicLong totalMessageSize = new AtomicLong();

  public MqttEstimatedTimetableSourceHiveMQ(MqttSiriETUpdaterParameters parameters) {
    this.parameters = parameters;
    this.primingExecutor = Executors.newFixedThreadPool(PRIMING_THREADS);
    this.liveExecutor = Executors.newSingleThreadExecutor();
  }

  @Override
  public void start(Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer) {
    this.serviceDeliveryConsumer = serviceDeliveryConsumer;

    for (int i = 0; i < NUM_CLIENTS; i++) {
      clients.add(connectAndSubscribeToClient(i));
    }
    connectedAt = Instant.now();

    timestampOfLastHistoricDelivery = Instant.now().plusSeconds(30);
    List<CompletableFuture<Void>> primingFutures = new ArrayList<>();

    for (int i = 0; i < PRIMING_THREADS; i++) {
      CompletableFuture<Void> f = CompletableFuture.runAsync(
        new PrimeRunner(),
        primingExecutor
      );
      primingFutures.add(f);
    }

    // Wait for *all* priming workers to finish
    CompletableFuture<Void> allPriming = CompletableFuture.allOf(
      primingFutures.toArray(new CompletableFuture[0])
    );

    // when all are done, switch to live
    allPriming.thenRunAsync(() -> {
      LOG.info("All priming workers done, starting live worker");
      liveExecutor.submit(new LiveRunner());
      primingExecutor.shutdown();
    }).exceptionally(ex -> {
      LOG.error("Priming failed", ex);
      return null;
    });

  }

  private Mqtt5AsyncClient connectAndSubscribeToClient(int clientIndex) {
    Mqtt5AsyncClient client = Mqtt5Client.builder()
      .identifier("OpenTripPlanner-" + clientIndex + "-" + UUID.randomUUID())
      .serverHost(parameters.host())
      .serverPort(parameters.port())
//      .simpleAuth()
//      .username(parameters.user())
//      .password(parameters.password().getBytes(StandardCharsets.UTF_8))
//      .applySimpleAuth()
      .automaticReconnectWithDefaultConfig()
      .addConnectedListener(ctx -> onConnect(clientIndex))
      .addDisconnectedListener(ctx -> onDisconnect(clientIndex, ctx))
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

  private void onDisconnect(int clientIndex, MqttClientDisconnectedContext ctx) {
    LOG.info("Disconnected client {} from MQTT broker: {}",
      clientIndex, parameters.url(), ctx.getCause());
  }

  private void onConnect(int clientIndex) {
    LOG.info("Connected client {} to MQTT broker: {} with qos: {}",
      clientIndex, parameters.url(), parameters.qos());
  }

  @Override
  public boolean isPrimed() {
    return true;
  }

  @Override
  public void teardown() {
    clients.forEach(Mqtt5AsyncClient::disconnect);
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
    if (message.isRetain()) {
      retainedMessageCounter.incrementAndGet();
    }
    int numberOfMessages = primedMessageCounter.incrementAndGet();
    long sizeBytes = totalMessageSize.addAndGet(message.getPayloadAsBytes().length);
    boolean offer = messageQueue.offer(message.getPayloadAsBytes());
    if (!offer) {
      LOG.warn("Failed to offer to message queue");
    }
    if (!primed && numberOfMessages % 1000 == 0) {
      long totalMillis = connectedAt.until(Instant.now(), ChronoUnit.MILLIS);
      double messageRate = (double) numberOfMessages / totalMillis * 1000;
      double sizeMB = sizeBytes / 1024. / 1024.;
      double meanMessageSizeKB = (double) sizeBytes / numberOfMessages / 1024.;
      LOG.info("Received {} messages ({} MB, {} retained) during priming." +
          " Mean message rate: {} per second. Mean message size: {} kB",
        numberOfMessages,
        String.format("%.2f", sizeMB),
        retainedMessageCounter.get(),
        String.format("%.2f", messageRate),
        String.format("%.2f", meanMessageSizeKB)
      );
      LOG.info("Queue size: {}", messageQueue.size());
    }
  }


  private class PrimeRunner implements Runnable {

    private static final Duration THRESHOLD_HISTORIC_DATA = Duration.ofMinutes(5);
    private static final int SECONDS_SINCE_LAST_HISTORIC_DELIVERY = 10;

    @Override
    public void run() {
      try {
        while (!primed && !Thread.currentThread().isInterrupted()) {
          byte[] payload = messageQueue.take();
          var optionalServiceDelivery = serviceDelivery(payload);
          if (optionalServiceDelivery.isEmpty()) {
            return;
          }
          ServiceDelivery serviceDelivery = optionalServiceDelivery.get();
          if (
            serviceDelivery.getResponseTimestamp()
              .plus(THRESHOLD_HISTORIC_DATA)
              .isBefore(ZonedDateTime.now())
          ) {
            timestampOfLastHistoricDelivery = Instant.now();
          }
          serviceDeliveryConsumer.apply(serviceDelivery);
          if (
            timestampOfLastHistoricDelivery
              .plusSeconds(SECONDS_SINCE_LAST_HISTORIC_DELIVERY)
              .isBefore(Instant.now())
          ) {
            double sizeMB = totalMessageSize.get() / 1024. / 1024.;
            double meanMessageSizeKB = sizeMB / primedMessageCounter.get() * 1024.;
            long totalMillis = connectedAt.until(Instant.now(), ChronoUnit.MILLIS);
            double messageRate = (double) primedMessageCounter.get() / totalMillis * 1000;
            LOG.info("Initial service delivery processing of {} messages complete. " +
                "Total size: {} MB, mean message size: {} kB, mean message rate: {} per second.",
              primedMessageCounter.get(),
              String.format("%.2f", sizeMB),
              String.format("%.2f", meanMessageSizeKB),
              String.format("%.2f", messageRate)
            );
            primed = true;
          }
        }
        LOG.info("Priming worker done");
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
