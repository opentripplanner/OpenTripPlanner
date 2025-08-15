package org.opentripplanner.ext.siri.updater.mqtt;

import jakarta.xml.bind.JAXBException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Future;
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
    // return primed;
    // consumption of initial data is currently too slow, return always true so the application
    // is still starting.
    // ToDo: make initial data consumption faster so that prime can be returned here
    return true;
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

    private final ArrayList<ServiceDelivery> initialServiceDeliveries = new ArrayList<>(50000);
    private static final Duration THRESHOLD_HISTORIC_DATA = Duration.ofMinutes(5);
    private static final int SECONDS_SINCE_LAST_HISTORIC_DELIVERY = 7;
    private Instant timestampOfLastHistoricDelivery;

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
      LOG.warn("Connection to MQTT broker lost: {}", parameters.url());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
      var serviceDeliveryOptional = serviceDelivery(message.getPayload());
      if (serviceDeliveryOptional.isEmpty()) {
        return;
      }

      var serviceDelivery = serviceDeliveryOptional.get();
      if (primed) {
        serviceDeliveryConsumer.apply(serviceDelivery);
        return;
      }

      initialServiceDeliveries.add(serviceDelivery);
      if (initialServiceDeliveries.size() % 500 == 0) {
        LOG.info("Service deliveries received: {}", initialServiceDeliveries.size());
      }

      if (serviceDelivery.getResponseTimestamp().plus(THRESHOLD_HISTORIC_DATA).isBefore(ZonedDateTime.now())) {
        timestampOfLastHistoricDelivery = Instant.now();
      }

      if (timestampOfLastHistoricDelivery.plusSeconds(SECONDS_SINCE_LAST_HISTORIC_DELIVERY).isBefore(Instant.now())) {
        LOG.info("Initial service delivery completed, start processing of {} messages",  initialServiceDeliveries.size());
        initialServiceDeliveries.forEach(serviceDeliveryConsumer::apply);
        LOG.info("Initial service delivery processing complete");
        primed = true;
      }
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
