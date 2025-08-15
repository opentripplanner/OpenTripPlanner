package org.opentripplanner.ext.siri.updater.mqtt;

import jakarta.xml.bind.JAXBException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
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
  private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

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
      connOpts.setCleanSession(true);
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
  public boolean isPrimed() {
    return primed;
  }

  @Override
  public void teardown() {
    try {
      client.disconnect();
    } catch (MqttException e) {
      LOG.error("Error disconnecting", e);
    }
  }

  private class Callback implements MqttCallbackExtended {

    private final Semaphore limit = new Semaphore(64);

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
      LOG.info("Connected to MQTT broker: {}", serverURI);
      try {
        client.subscribe(parameters.topic(), parameters.qos());
      } catch (MqttException e) {
        LOG.warn("Could not subscribe to: {}", parameters.topic());
      }
    }

    @Override
    public void connectionLost(Throwable cause) {
      LOG.warn("Connection to MQTT broker lost: {}", parameters.url());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
      if (limit.tryAcquire()) {
        try {
          virtualThreadExecutor.submit(() ->
            serviceDelivery(message.getPayload()).ifPresent(serviceDeliveryConsumer::apply)
          );
        } finally {
          limit.release();
        }
      } else {
        serviceDelivery(message.getPayload()).ifPresent(serviceDeliveryConsumer::apply);
      }
      primed = true; // ToDo figure out how determine if all historic messages where processed
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}

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
