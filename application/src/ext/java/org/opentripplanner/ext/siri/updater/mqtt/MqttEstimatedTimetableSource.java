package org.opentripplanner.ext.siri.updater.mqtt;

import static org.opentripplanner.utils.lang.StringUtils.hasNoValue;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import jakarta.xml.bind.JAXBException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

/**
 * This is a realtime updater for trip updates in Siri ET format via MQTT. The updater is primed
 * (ready for routing requests), when all retained messages in the connected MQTT are processed.
 * If there are no retained messages, the updater is primed immediately. Live messages (messages
 * without a retained flag) are always processed, even if the updater is not yet primed.
 * <p>
 * Readiness is decoupled from the connection lifecycle: a watcher marks the updater primed
 * {@code connectionStartupTimeout} after startup regardless of the connection state. This
 * guarantees OTP eventually becomes ready even if the broker is never reachable, or if the
 * connection repeatedly fails during the MQTT handshake (e.g. the server closes the connection
 * without ever sending a CONNACK, so {@link #onConnect()} never fires) and HiveMQ keeps retrying
 * in a connect/disconnect loop. The MQTT client continues retrying in the background; once the
 * broker becomes available live messages are processed normally, even after the updater has been
 * marked primed.
 */
public class MqttEstimatedTimetableSource implements AsyncEstimatedTimetableSource {

  private static final Logger LOG = LoggerFactory.getLogger(MqttEstimatedTimetableSource.class);

  private final MqttSiriETUpdaterParameters parameters;

  private Mqtt5AsyncClient client;
  private Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer;
  private final List<Future<?>> graphUpdates = Collections.synchronizedList(new ArrayList<>());

  private final BlockingQueue<byte[]> liveMessageQueue = new LinkedBlockingQueue<>();
  private final BlockingQueue<byte[]> primingMessageQueue = new LinkedBlockingQueue<>();
  private final ExecutorService primingExecutor;
  private final ExecutorService liveExecutor;
  private final ScheduledExecutorService readinessWatcher;

  private volatile boolean primed = false;

  /**
   * Guards the one-time transition from startup into the priming phase, which is triggered by the
   * first successful connection (see {@link #onConnect()}).
   */
  private final AtomicBoolean primingStarted = new AtomicBoolean(false);

  private Instant connectedAt;
  private final AtomicLong liveMessageCounter = new AtomicLong();
  private final AtomicLong primingMessageCounter = new AtomicLong();
  private final AtomicLong liveMessageSize = new AtomicLong();
  private final AtomicLong primingMessageSize = new AtomicLong();
  private final AtomicLong processedLiveMessageCounter = new AtomicLong();
  private final AtomicLong processedPrimingMessageCounter = new AtomicLong();

  public MqttEstimatedTimetableSource(MqttSiriETUpdaterParameters parameters) {
    this.parameters = parameters;

    ThreadFactory primingThreadFactory = Thread.ofPlatform()
      .name("primingSiriMqttUpdater-", 0)
      .factory();
    this.primingExecutor = Executors.newFixedThreadPool(
      parameters.numberOfPrimingWorkers(),
      primingThreadFactory
    );

    ThreadFactory liveThreadFactory = Thread.ofPlatform().name("liveSiriMqttUpdater-", 0).factory();
    this.liveExecutor = Executors.newSingleThreadExecutor(liveThreadFactory);

    ThreadFactory watcherThreadFactory = Thread.ofPlatform()
      .name("readinessSiriMqttUpdater-", 0)
      .factory();
    this.readinessWatcher = Executors.newSingleThreadScheduledExecutor(watcherThreadFactory);

    registerMetrics();
  }

  @Override
  public void start(Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer) {
    this.serviceDeliveryConsumer = serviceDeliveryConsumer;

    // Fallback readiness for when no connection is ever established: if the broker is never
    // reachable, or the connection keeps failing during the MQTT handshake (connect/disconnect
    // loop, so onConnect never fires and priming never starts), this watcher guarantees OTP
    // eventually becomes ready. Once a connection is established and priming has started, readiness
    // is governed by priming completion instead (see onConnect -> beginPriming), so a slow but
    // progressing backlog is not cut short by this timeout.
    readinessWatcher.schedule(
      this::markPrimedOnStartupTimeout,
      parameters.connectionStartupTimeout().toMillis(),
      TimeUnit.MILLISECONDS
    );

    client = buildAndConnectClient();
  }

  private void markPrimedOnStartupTimeout() {
    if (primingStarted.get()) {
      // A connection was established and priming is underway; let it finish and mark readiness on
      // completion rather than forcing it here.
      return;
    }
    if (!primed) {
      LOG.warn(
        "MQTT broker at {} did not connect within {}. " +
          "OTP will start routing without real-time data. " +
          "The MQTT client will keep retrying in the background.",
        parameters.url(),
        parameters.connectionStartupTimeout()
      );
      primed = true;
    }
  }

  /**
   * Start the priming workers that drain the retained-message backlog. Triggered by the first
   * successful connection and guarded so it runs at most once, even across reconnects. When all
   * workers have idled out and their graph updates are applied, the live runner is started and the
   * updater is marked primed (this may happen before the readiness watcher fires).
   * <p>
   * Live (non-retained) messages that arrive during priming are buffered in the live queue and only
   * applied once the retained backlog has been fully processed. This guarantees priming-first
   * ordering: an older retained update can never overwrite a newer live update in the graph.
   */
  private void beginPriming() {
    if (!primingStarted.compareAndSet(false, true)) {
      return;
    }

    List<CompletableFuture<Void>> primingFutures = new ArrayList<>();
    for (int i = 0; i < parameters.numberOfPrimingWorkers(); i++) {
      CompletableFuture<Void> f = CompletableFuture.runAsync(new RetainRunner(i), primingExecutor);
      primingFutures.add(f);
    }
    LOG.info("Started {} priming workers", parameters.numberOfPrimingWorkers());

    CompletableFuture.allOf(primingFutures.toArray(new CompletableFuture[0]))
      .thenRunAsync(() -> {
        waitForGraphUpdates();
        logPrimingSummary();
        primingExecutor.shutdown();
        // Only now start consuming live messages, so the retained backlog is fully applied first.
        liveExecutor.submit(new LiveRunner());
        primed = true;
      })
      .exceptionally(ex -> {
        LOG.error("Priming failed", ex);
        return null;
      });
  }

  private void waitForGraphUpdates() {
    for (Future<?> f : graphUpdates) {
      try {
        f.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
    graphUpdates.clear();
  }

  private void registerMetrics() {
    FunctionCounter.builder("mqtt.siri.message.size", liveMessageSize, AtomicLong::get)
      .tags("type", "live", "stage", "received")
      .register(Metrics.globalRegistry);
    FunctionCounter.builder("mqtt.siri.message.size", primingMessageSize, AtomicLong::get)
      .tags("type", "priming", "stage", "received")
      .register(Metrics.globalRegistry);
    FunctionCounter.builder("mqtt.siri.messages", liveMessageCounter, AtomicLong::get)
      .tags("type", "live", "stage", "received")
      .register(Metrics.globalRegistry);
    FunctionCounter.builder("mqtt.siri.messages", primingMessageCounter, AtomicLong::get)
      .tags("type", "priming", "stage", "received")
      .register(Metrics.globalRegistry);
    FunctionCounter.builder("mqtt.siri.messages", processedLiveMessageCounter, AtomicLong::get)
      .tags("type", "live", "stage", "processed")
      .register(Metrics.globalRegistry);
    FunctionCounter.builder("mqtt.siri.messages", processedPrimingMessageCounter, AtomicLong::get)
      .tags("type", "priming", "stage", "processed")
      .register(Metrics.globalRegistry);

    Gauge.builder("mqtt.siri.queue.size", primingMessageQueue, Collection::size)
      .tags("type", "priming")
      .register(Metrics.globalRegistry);
    Gauge.builder("mqtt.siri.queue.size", liveMessageQueue, Collection::size)
      .tags("type", "live")
      .register(Metrics.globalRegistry);
  }

  /**
   * Build the HiveMQ client and initiate a non-blocking connect. The client will automatically
   * retry the connection (with exponential backoff) if the broker is unavailable. When a
   * connection is established, {@link #onConnect()} is called which sets up the subscription.
   */
  private Mqtt5AsyncClient buildAndConnectClient() {
    Mqtt5SimpleAuth auth;
    if (hasNoValue(parameters.user()) || hasNoValue(parameters.password())) {
      auth = null;
    } else {
      auth = Mqtt5SimpleAuth.builder()
        .username(parameters.user())
        .password(parameters.password().getBytes(StandardCharsets.UTF_8))
        .build();
    }
    Mqtt5AsyncClient mqttClient = Mqtt5Client.builder()
      .identifier("OpenTripPlanner-" + UUID.randomUUID())
      .serverHost(parameters.host())
      .serverPort(parameters.port())
      .simpleAuth(auth)
      .automaticReconnectWithDefaultConfig()
      .addConnectedListener(ctx -> onConnect())
      .addDisconnectedListener(this::onDisconnect)
      .buildAsync();

    // Non-blocking connect: HiveMQ will retry automatically if the broker is unavailable.
    mqttClient.connectWith().keepAlive(30).cleanStart(false).send();

    return mqttClient;
  }

  private void onDisconnect(MqttClientDisconnectedContext ctx) {
    LOG.info("Disconnected client from MQTT broker: {}", parameters.url(), ctx.getCause());
  }

  private void onConnect() {
    connectedAt = Instant.now();
    LOG.info(
      "Connected client to MQTT broker: {} with qos: {}",
      parameters.url(),
      parameters.qos()
    );
    // Subscribe on every connect (including reconnects). With cleanStart=false the broker
    // remembers the subscription across reconnects, but re-subscribing is idempotent in MQTT5
    // and ensures the callback is always registered correctly.
    client
      .subscribeWith()
      .topicFilter(parameters.topic())
      .qos(Optional.ofNullable(MqttQos.fromCode(parameters.qos())).orElse(MqttQos.AT_MOST_ONCE))
      .callback(this::onMessage)
      .send();

    // Start draining the retained-message backlog on the first successful connection.
    beginPriming();
  }

  @Override
  public boolean isPrimed() {
    return primed;
  }

  @Override
  public void teardown() {
    readinessWatcher.shutdownNow();
    liveExecutor.shutdownNow();
    primingExecutor.shutdownNow();
    if (client != null) {
      client.disconnect();
    }
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
      primingMessageCounter.incrementAndGet();
      primingMessageSize.addAndGet(message.getPayloadAsBytes().length);
    } else {
      offer = liveMessageQueue.offer(message.getPayloadAsBytes());
      liveMessageCounter.incrementAndGet();
      liveMessageSize.addAndGet(message.getPayloadAsBytes().length);
    }

    if (!offer) {
      LOG.warn("Failed to offer to message queue");
    }

    if (!primed && (primingMessageCounter.get() + liveMessageCounter.get()) % 1000 == 0) {
      // TODO: Better as metric and not log
      logMessageRates();
      LOG.info(
        "Retained message queue size: {}, live message queue size: {}",
        primingMessageQueue.size(),
        liveMessageQueue.size()
      );
    }
  }

  private void logMessageRates() {
    long totalMillis = connectedAt.until(Instant.now(), ChronoUnit.MILLIS);

    long receivedLiveMessageCount = liveMessageCounter.get();
    long processedLiveMessageCount = processedLiveMessageCounter.get();
    double receivedLiveMessageRate = ((double) receivedLiveMessageCount / totalMillis) * 1000;
    double processedLiveMessageRate = ((double) processedLiveMessageCount / totalMillis) * 1000;
    LOG.info(
      "Siri Messages: Processed/Received {}/{} live messages ({} /s received, {} /s processed",
      processedLiveMessageCount,
      receivedLiveMessageCount,
      String.format("%.2f", receivedLiveMessageRate),
      String.format("%.2f", processedLiveMessageRate)
    );

    long receivedPrimingMessageCount = primingMessageCounter.get();
    long processedPrimingMessageCount = processedPrimingMessageCounter.get();
    double receivedPrimingMessageRate = ((double) receivedPrimingMessageCount / totalMillis) * 1000;
    double processedPrimingMessageRate =
      ((double) processedPrimingMessageCount / totalMillis) * 1000;
    LOG.info(
      "Siri Messages: Processed/Received {}/{} retained messages ({} /s received, {} /s processed",
      processedPrimingMessageCount,
      receivedPrimingMessageCount,
      String.format("%.2f", receivedPrimingMessageRate),
      String.format("%.2f", processedPrimingMessageRate)
    );
  }

  private void logPrimingSummary() {
    LOG.info(
      "All priming workers done after {} seconds",
      connectedAt.until(Instant.now(), ChronoUnit.SECONDS)
    );

    long messageCount = primingMessageCounter.get();
    long totalMessageSize = primingMessageSize.get();
    double sizeMb = totalMessageSize / 1024. / 1024.;
    double meanMessageSizeKB = totalMessageSize / (double) messageCount / 1024.;
    long totalMillis = connectedAt.until(Instant.now(), ChronoUnit.MILLIS);
    double messageRate = ((double) messageCount / totalMillis) * 1000;
    LOG.info(
      "Processed retained {} messages. Total size: {} MB, mean message size: {} kB, mean message rate: {} per second.",
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
          byte[] payload = primingMessageQueue.poll(
            parameters.maxPrimingIdleTime().toSeconds(),
            TimeUnit.SECONDS
          );
          if (payload == null) {
            LOG.info(
              "RetainRunner-{} was idle for {} seconds and shut down after processing {} messages.",
              workerId,
              parameters.maxPrimingIdleTime().toSeconds(),
              runnerMessageCounter.get()
            );
            break;
          }
          processedPrimingMessageCounter.incrementAndGet();
          runnerMessageCounter.incrementAndGet();
          var optionalServiceDelivery = serviceDelivery(payload);
          if (optionalServiceDelivery.isEmpty()) {
            continue;
          }
          var serviceDelivery = optionalServiceDelivery.get();
          graphUpdates.add(serviceDeliveryConsumer.apply(serviceDelivery));
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        LOG.error("Error while processing Siri ET update during priming.", e);
      }
    }
  }

  private class LiveRunner implements Runnable {

    @Override
    public void run() {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          byte[] payload = liveMessageQueue.take();
          processedLiveMessageCounter.incrementAndGet();
          serviceDelivery(payload).ifPresent(serviceDeliveryConsumer::apply);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        LOG.error("Error while processing Siri ET update.", e);
      }
    }
  }
}
