package org.opentripplanner.ext.siri.updater.mqtt;

import jakarta.xml.bind.JAXBException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

  private Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer;
  private volatile boolean primed;

  private MqttClient client;

  public MqttEstimatedTimetableSource(MqttSiriETUpdaterParameters parameters) {
    this.parameters = parameters;
  }

  @Override
  public void start(@Nonnull Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer) {
    this.serviceDeliveryConsumer = serviceDeliveryConsumer;

    try {
      String clientId = "OpenTripPlanner-" + MqttClient.generateClientId();
      MemoryPersistence persistence = new MemoryPersistence();
      client = new MqttClient(parameters.url(), clientId, persistence);

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

  private class Callback implements MqttCallbackExtended {

    private static final Duration THRESHOLD_HISTORIC_DATA = Duration.ofMinutes(5);
    private static final int SECONDS_SINCE_LAST_HISTORIC_DELIVERY = 7;
    public static final int MAX_PRIMING_THREADS = 1;
    private final BlockingQueue<byte[]> messageQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger primedMessageCounter = new AtomicInteger();
    private volatile Instant timestampOfLastHistoricDelivery;

    public Callback() {
      ThreadPoolExecutor primingExecutor = new ThreadPoolExecutor(
        0,
        MAX_PRIMING_THREADS,
        30L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        Executors.defaultThreadFactory()
      );
      primingExecutor.submit(new PrimeRunner());
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
      LOG.info("Connected to MQTT broker: {}", serverURI);
      timestampOfLastHistoricDelivery = Instant.now();
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
      int numberOfMessages = primedMessageCounter.incrementAndGet();
      messageQueue.put(message.getPayload());

      if (!primed && numberOfMessages % 1000 == 0) {
        LOG.info("Received {} messages during priming", numberOfMessages);
      }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    private class PrimeRunner implements Runnable {

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
            if (serviceDelivery.getResponseTimestamp().plus(THRESHOLD_HISTORIC_DATA).isBefore(ZonedDateTime.now())) {
              timestampOfLastHistoricDelivery = Instant.now();
            }
            serviceDeliveryConsumer.apply(serviceDelivery);
            if (timestampOfLastHistoricDelivery.plusSeconds(SECONDS_SINCE_LAST_HISTORIC_DELIVERY).isBefore(Instant.now())) {
              LOG.info("Initial service delivery processing of {} messages complete", primedMessageCounter.get());
              primed = true;
              startLiveWorker();
            }
          }
          LOG.info("Priming worker done");
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

    private void startLiveWorker() {
      Thread worker = new Thread(() -> {
        LOG.info("Live worker started");
        try {
          while (!Thread.currentThread().isInterrupted()) {
            var serviceDeliveryOptional = serviceDelivery(messageQueue.take());
            serviceDeliveryOptional.ifPresent(serviceDeliveryConsumer::apply);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }, "mqtt-siri-live-worker");
      worker.setDaemon(true);
      worker.start();
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
  }
}
