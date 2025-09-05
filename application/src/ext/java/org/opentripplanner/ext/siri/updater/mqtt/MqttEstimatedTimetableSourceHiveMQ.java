package org.opentripplanner.ext.siri.updater.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import jakarta.xml.bind.JAXBException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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

  private final BlockingQueue<byte[]> liveMessageQueue = new LinkedBlockingQueue<>();
  private final BlockingQueue<byte[]> primingMessageQueue = new LinkedBlockingQueue<>();
  private final ExecutorService primingExecutor;
  private final ExecutorService liveExecutor;

  private volatile boolean primed = false;

  private Instant connectedAt;
  private final AtomicInteger liveMessageCounter = new AtomicInteger();
  private final AtomicInteger retainedMessageCounter = new AtomicInteger();
  private final AtomicLong liveMessageSize = new AtomicLong();
  private final AtomicLong retainedMessageSize = new AtomicLong();

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

    List<CompletableFuture<Void>> primingFutures = new ArrayList<>();

    for (int i = 0; i < parameters.numberOfPrimingWorkers(); i++) {
      CompletableFuture<Void> f = CompletableFuture.runAsync(
        new RetainRunner(i),
        ForkJoinPool.commonPool()
      );
      primingFutures.add(f);
    }
    LOG.info("Started {} priming workers", parameters.numberOfPrimingWorkers());
    liveExecutor.submit(new LiveRunner());

    // Wait for priming workers to finish
    CompletableFuture<Void> allPriming = CompletableFuture.allOf(
      primingFutures.toArray(new CompletableFuture[0])
    );

    // when all are done, switch to live
    allPriming.thenRunAsync(() -> {
      logPrimingSummary();
      primingExecutor.shutdown();
      primed = true;
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
      .cleanStart(false)
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
    return true;  // ToDo: when done optimizing, set to primed
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
    boolean offer;
    if (message.isRetain() && !primed) {
      offer = primingMessageQueue.offer(message.getPayloadAsBytes());
      retainedMessageCounter.incrementAndGet();
      retainedMessageSize.addAndGet(message.getPayloadAsBytes().length);
    } else {
      offer = liveMessageQueue.offer(message.getPayloadAsBytes());
      liveMessageCounter.incrementAndGet();
      liveMessageSize.addAndGet(message.getPayloadAsBytes().length);
    }

    if (!offer) {
      LOG.warn("Failed to offer to message queue");
    }

    if (!primed && (retainedMessageCounter.get() + liveMessageCounter.get()) % 1000 == 0) {
      logMessageRates();  // ToDo: Better as metric and not log
      LOG.info("Retained message queue size: {}, live message queue size: {}",
        primingMessageQueue.size(), liveMessageQueue.size());
    }
  }

  private void logMessageRates() {
    long totalMillis = connectedAt.until(Instant.now(), ChronoUnit.MILLIS);

    int receivedLiveMessageCount = liveMessageCounter.get();
    int processedLiveMessageCount = receivedLiveMessageCount - liveMessageQueue.size();
    double receivedLiveMessageRate = (double) receivedLiveMessageCount / totalMillis * 1000;
    double processedLiveMessageRate = (double) processedLiveMessageCount / totalMillis * 1000;
    LOG.info(
      "Siri Messages: Processed/Received {}/{} live messages ({} /s received, {} /s processed",
      processedLiveMessageCount,
      receivedLiveMessageCount,
      String.format("%.2f", receivedLiveMessageRate),
      String.format("%.2f", processedLiveMessageRate)
    );

    int receivedRetainedMessageCount = retainedMessageCounter.get();
    int processedRetainedMessageCount = receivedRetainedMessageCount - primingMessageQueue.size();
    double receivedRetainedMessageRate = (double) receivedRetainedMessageCount / totalMillis * 1000;
    double processedRetainedMessageRate = (double) processedRetainedMessageCount / totalMillis * 1000;
    LOG.info(
      "Siri Messages: Processed/Received {}/{} retained messages ({} /s received, {} /s processed",
      processedRetainedMessageCount,
      receivedRetainedMessageCount,
      String.format("%.2f", receivedRetainedMessageRate),
      String.format("%.2f", processedRetainedMessageRate)
    );
  }


  private void logPrimingSummary() {
    LOG.info("All priming workers done after {} seconds",
      connectedAt.until(Instant.now(), ChronoUnit.SECONDS));

    int messageCount = retainedMessageCounter.get();
    long totalMessageSize = retainedMessageSize.get();
    double sizeMb = totalMessageSize / 1024. / 1024.;
    double meanMessageSizeKB = totalMessageSize / (double) messageCount / 1024.;
    long totalMillis = connectedAt.until(Instant.now(), ChronoUnit.MILLIS);
    double messageRate = (double) messageCount / totalMillis * 1000;
    LOG.info("Processed retained {} messages. Total size: {} MB, mean message size: {} kB, mean message rate: {} per second.",
      messageCount,
      String.format("%.2f", sizeMb),
      String.format("%.2f", meanMessageSizeKB),
      String.format("%.2f", messageRate)
    );
  }

  private class RetainRunner implements Runnable {

    private final int workerId;

    private final AtomicInteger runnerMessageCounter = new AtomicInteger(0);

    public RetainRunner(int workerId) {
      this.workerId = workerId;
    }

    @Override
    public void run() {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          byte[] payload = primingMessageQueue.poll(parameters.maxPrimingIdleTime().toSeconds(), TimeUnit.SECONDS);
          if (payload == null) {
            LOG.info(
              "RetainRunner-{} was idle for {} seconds and shut down after processing {} messages.",
              workerId, parameters.maxPrimingIdleTime().toSeconds(), runnerMessageCounter.get()
            );
            break;
          }
          runnerMessageCounter.incrementAndGet();
          ForkJoinPool.commonPool().execute(() -> {
            var optionalServiceDelivery = serviceDelivery(payload);
            if (optionalServiceDelivery.isEmpty()) {
              return;
            }
            var serviceDelivery = optionalServiceDelivery.get();
            serviceDeliveryConsumer.apply(serviceDelivery);
          });
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private class LiveRunner implements Runnable {
    @Override
    public void run() {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          byte[] payload = liveMessageQueue.take();
          serviceDelivery(payload).ifPresent(serviceDeliveryConsumer::apply);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
