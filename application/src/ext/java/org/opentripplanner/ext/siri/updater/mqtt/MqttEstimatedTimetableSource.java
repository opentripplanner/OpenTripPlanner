package org.opentripplanner.ext.siri.updater.mqtt;

import jakarta.xml.bind.JAXBException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLStreamException;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.entur.siri21.util.SiriXml;
import org.opentripplanner.updater.trip.siri.updater.AsyncEstimatedTimetableSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.ServiceDelivery;
import uk.org.siri.siri21.Siri;

public class MqttEstimatedTimetableSource implements AsyncEstimatedTimetableSource {

  private static final Logger LOG = LoggerFactory.getLogger(MqttEstimatedTimetableSource.class);

  private final MqttSiriETUpdaterParameters parameters;

  private MqttClient client;

    private Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer;

  private final BlockingQueue<byte[]> liveMessageQueue = new LinkedBlockingQueue<>();
  private final BlockingQueue<byte[]> primingMessageQueue = new LinkedBlockingQueue<>();
  private final ExecutorService primingExecutor;
  private final ExecutorService liveExecutor;

  private volatile boolean primed = false;

  private Instant connectedAt;
  private final AtomicLong liveMessageCounter = new AtomicLong();
  private final AtomicLong primingMessageCounter = new AtomicLong();
  private final AtomicLong liveMessageSize = new AtomicLong();
  private final AtomicLong primingMessageSize = new AtomicLong();
  private final AtomicLong processedLiveMessageCounter = new AtomicLong();
  private final AtomicLong processedPrimingMessageCounter = new AtomicLong();

  public MqttEstimatedTimetableSource(MqttSiriETUpdaterParameters parameters) {
    this.parameters = parameters;
    this.primingExecutor = Executors.newFixedThreadPool(parameters.numberOfPrimingWorkers());
    this.liveExecutor = Executors.newSingleThreadExecutor();
  }

  @Override
  public void start(@Nonnull Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer) {
    this.serviceDeliveryConsumer = serviceDeliveryConsumer;

    connectAndSubscribeToClient();

    connectedAt = Instant.now();

    List<CompletableFuture<Void>> primingFutures = new ArrayList<>();

    for (int i = 0; i < parameters.numberOfPrimingWorkers(); i++) {
      CompletableFuture<Void> f = CompletableFuture.runAsync(
        new RetainRunner(i),
        primingExecutor
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

  private void connectAndSubscribeToClient() {
    try {
      String clientId = "OpenTripPlanner-" + MqttClient.generateClientId();
      MemoryPersistence persistence = new MemoryPersistence();
      client = new MqttClient("tcp://" + parameters.url(), clientId, persistence);

      MqttConnectOptions connOpts = new MqttConnectOptions();
      connOpts.setMaxInflight(100);
      connOpts.setCleanSession(false);
      connOpts.setAutomaticReconnect(true);
      if (parameters.user() != null && parameters.password() != null) {
        connOpts.setUserName(parameters.user());
        connOpts.setPassword(parameters.password().toCharArray());
      }
      client.setCallback(new Callback());

      LOG.debug("Connecting to broker: {}", parameters.url());
      client.connect(connOpts);
    } catch (MqttException e) {
      LOG.warn("Failed to connect to broker: {}", parameters.url(), e);
    }
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
  public boolean isPrimed() {
    // return primed;
    // consumption of initial data is currently too slow, return always true so the application
    // is still starting.
    // ToDo: make initial data consumption faster so that prime can be returned here
    return true;
  }

  private void logPrimingSummary() {
    LOG.info("All priming workers done after {} seconds",
      connectedAt.until(Instant.now(), ChronoUnit.SECONDS));

    long messageCount = primingMessageCounter.get();
    long totalMessageSize = primingMessageSize.get();
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

  private void logMessageRates() {
    long totalMillis = connectedAt.until(Instant.now(), ChronoUnit.MILLIS);

    long receivedLiveMessageCount = liveMessageCounter.get();
    long processedLiveMessageCount = processedLiveMessageCounter.get();
    double receivedLiveMessageRate = (double) receivedLiveMessageCount / totalMillis * 1000;
    double processedLiveMessageRate = (double) processedLiveMessageCount / totalMillis * 1000;
    LOG.info(
      "Siri Messages: Processed/Received {}/{} live messages ({} /s received, {} /s processed",
      processedLiveMessageCount,
      receivedLiveMessageCount,
      String.format("%.2f", receivedLiveMessageRate),
      String.format("%.2f", processedLiveMessageRate)
    );

    long receivedPrimingMessageCount = primingMessageCounter.get();
    long processedPrimingMessageCount = processedPrimingMessageCounter.get();
    double receivedPrimingMessageRate = (double) receivedPrimingMessageCount / totalMillis * 1000;
    double processedPrimingMessageRate = (double) processedPrimingMessageCount / totalMillis * 1000;
    LOG.info(
      "Siri Messages: Processed/Received {}/{} retained messages ({} /s received, {} /s processed",
      processedPrimingMessageCount,
      receivedPrimingMessageCount,
      String.format("%.2f", receivedPrimingMessageRate),
      String.format("%.2f", processedPrimingMessageRate)
    );
  }

  private class Callback implements MqttCallbackExtended {

    public Callback() {
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
      LOG.info("Connected to MQTT broker: {}", serverURI);
      connectedAt = Instant.now();
      try {
        client.subscribe(parameters.topic(), parameters.qos());
      } catch (MqttException e) {
        LOG.warn("Could not subscribe to: {}", parameters.topic());
      }
    }

    @Override
    public void connectionLost(Throwable cause) {
      LOG.warn("Connection to MQTT broker lost: {}", parameters.url(), cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws InterruptedException {
      boolean offer;
      if (message.isRetained() && !primed) {
        offer = primingMessageQueue.offer(message.getPayload());
        primingMessageCounter.incrementAndGet();
        primingMessageSize.addAndGet(message.getPayload().length);
      } else {
        offer = liveMessageQueue.offer(message.getPayload());
        liveMessageCounter.incrementAndGet();
        liveMessageSize.addAndGet(message.getPayload().length);
      }

      if (!offer) {
        LOG.warn("Failed to offer to message queue");
      }

      if (!primed && (primingMessageCounter.get() + liveMessageCounter.get()) % 1000 == 0) {
        logMessageRates();  // ToDo: Better as metric and not log
        LOG.info("Retained message queue size: {}, live message queue size: {}",
          primingMessageQueue.size(), liveMessageQueue.size());
      }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
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
          processedPrimingMessageCounter.incrementAndGet();
          runnerMessageCounter.incrementAndGet();
          var optionalServiceDelivery = serviceDelivery(payload);
          if (optionalServiceDelivery.isEmpty()) {
            continue;
          }
          var serviceDelivery = optionalServiceDelivery.get();
          serviceDeliveryConsumer.apply(serviceDelivery);
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
          processedLiveMessageCounter.incrementAndGet();
          serviceDelivery(payload).ifPresent(serviceDeliveryConsumer::apply);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
