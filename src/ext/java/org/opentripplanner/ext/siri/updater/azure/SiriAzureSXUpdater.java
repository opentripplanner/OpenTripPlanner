package org.opentripplanner.ext.siri.updater.azure;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import jakarta.xml.bind.JAXBException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamException;
import org.apache.hc.core5.net.URIBuilder;
import org.opentripplanner.ext.siri.SiriAlertsUpdateHandler;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.alert.TransitAlertProvider;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ServiceDelivery;

public class SiriAzureSXUpdater extends AbstractAzureSiriUpdater implements TransitAlertProvider {

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private final SiriAlertsUpdateHandler updateHandler;
  private final TransitAlertService transitAlertService;

  private static final transient AtomicLong messageCounter = new AtomicLong(0);
  private final LocalDate fromDateTime;
  private final LocalDate toDateTime;

  public SiriAzureSXUpdater(SiriAzureSXUpdaterParameters config, TransitModel transitModel) {
    super(config, transitModel);
    this.fromDateTime = config.getFromDateTime();
    this.toDateTime = config.getToDateTime();
    this.transitAlertService = new TransitAlertServiceImpl(transitModel);
    this.updateHandler =
      new SiriAlertsUpdateHandler(
        feedId,
        transitModel,
        transitAlertService,
        fuzzyTripMatcher(),
        Duration.ZERO
      );
  }

  @Override
  protected void messageConsumer(ServiceBusReceivedMessageContext messageContext) {
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

  @Override
  protected void errorConsumer(ServiceBusErrorContext errorContext) {
    defaultErrorConsumer(errorContext);
  }

  @Override
  protected void initializeData(String url, Consumer<ServiceBusReceivedMessageContext> consumer)
    throws URISyntaxException {
    if (url == null) {
      LOG.info("No history url set up for Siri Azure SX Updater");
      return;
    }

    URI uri = new URIBuilder(url)
      .addParameter("publishFromDateTime", fromDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
      .addParameter("publishToDateTime", toDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
      .build();

    LOG.info("Fetching initial Siri SX data from {}, timeout is {} ms.", uri, timeout);
    var siri = fetchInitialSiriData(uri);

    if (siri.isEmpty()) {
      LOG.info("Got empty SX response from history endpoint");
      return;
    }

    // This is fine since runnables are scheduled after each other
    processHistory(siri.get());
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
    return super.saveResultOnGraph.execute((graph, transitModel) -> updateHandler.update(siriSx));
  }

  private void processHistory(ServiceDelivery siri) {
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
