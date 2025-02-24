package org.opentripplanner.ext.siri.updater.azure;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import jakarta.xml.bind.JAXBException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import javax.xml.stream.XMLStreamException;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.alert.TransitAlertProvider;
import org.opentripplanner.updater.siri.SiriAlertsUpdateHandler;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ServiceDelivery;

public class SiriAzureSXUpdater implements TransitAlertProvider, SiriAzureMessageHandler {

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private final SiriAlertsUpdateHandler updateHandler;
  private final TransitAlertService transitAlertService;

  private static final AtomicLong messageCounter = new AtomicLong(0);

  private WriteToGraphCallback saveResultOnGraph;

  public SiriAzureSXUpdater(
    SiriAzureSXUpdaterParameters config,
    TimetableRepository timetableRepository
  ) {
    this.transitAlertService = new TransitAlertServiceImpl(timetableRepository);
    this.updateHandler =
      new SiriAlertsUpdateHandler(config.feedId(), transitAlertService, Duration.ZERO);
  }

  @Override
  public void setup(WriteToGraphCallback writeToGraphCallback) {
    this.saveResultOnGraph = writeToGraphCallback;
  }

  @Override
  public void handleMessage(ServiceBusReceivedMessageContext messageContext) {
    var message = messageContext.getMessage();

    LOG.debug(
      "Processing message. messageId={}, sequenceNumber={}, enqueued time={}",
      message.getMessageId(),
      message.getSequenceNumber(),
      message.getEnqueuedTime()
    );

    messageCounter.incrementAndGet();

    try {
      var siriSx = parseSiriSx(message.getBody().toString(), message.getMessageId());
      if (siriSx.isEmpty()) {
        return;
      }
      processMessage(siriSx.get());
    } catch (JAXBException | XMLStreamException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

  private Optional<ServiceDelivery> parseSiriSx(String xmlMessage, String id)
    throws XMLStreamException, JAXBException {
    var siri = SiriXml.parseXml(xmlMessage);
    if (
      siri.getServiceDelivery() == null ||
      siri.getServiceDelivery().getSituationExchangeDeliveries() == null ||
      siri.getServiceDelivery().getSituationExchangeDeliveries().isEmpty()
    ) {
      if (siri.getHeartbeatNotification() != null) {
        LOG.debug("Received SIRI heartbeat message");
      } else {
        LOG.info("Empty Siri message for messageId {}", id);
      }
      return Optional.empty();
    }
    return Optional.of(siri.getServiceDelivery());
  }

  private Future<?> processMessage(ServiceDelivery siriSx) {
    return saveResultOnGraph.execute(context -> updateHandler.update(siriSx, context));
  }

  @Override
  public void processHistory(ServiceDelivery siri) {
    var sx = siri.getSituationExchangeDeliveries();

    if (sx == null || sx.isEmpty()) {
      LOG.info("Did not receive any SX messages from history endpoint");
      return;
    }

    try {
      var t1 = System.currentTimeMillis();
      var f = processMessage(siri);
      f.get();
      LOG.info("Azure SX updater initialized in {} ms.", (System.currentTimeMillis() - t1));
    } catch (ExecutionException | InterruptedException e) {
      throw new SiriAzureInitializationException("Error applying SX history", e);
    }
  }

  @Override
  public TransitAlertService getTransitAlertService() {
    return this.transitAlertService;
  }
}
