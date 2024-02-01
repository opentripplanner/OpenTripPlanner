package org.opentripplanner.ext.siri.updater.azure;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import jakarta.xml.bind.JAXBException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamException;
import org.apache.hc.core5.net.URIBuilder;
import org.opentripplanner.ext.siri.SiriAlertsUpdateHandler;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.alert.TransitAlertProvider;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

public class SiriAzureSXUpdater extends AbstractAzureSiriUpdater implements TransitAlertProvider {

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private final SiriAlertsUpdateHandler updateHandler;
  private final TransitAlertService transitAlertService;

  private static final transient AtomicLong messageCounter = new AtomicLong(0);
  private final LocalDate fromDateTime;
  private final LocalDate toDateTime;
  private Instant startTime;

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
    processMessage(message.getBody().toString(), message.getMessageId());
  }

  @Override
  protected void errorConsumer(ServiceBusErrorContext errorContext) {
    defaultErrorConsumer(errorContext);
  }

  @Override
  protected void initializeData(String url, Consumer<ServiceBusReceivedMessageContext> consumer)
    throws URISyntaxException {
    if (url == null) {
      LOG.info("No history url set up for Siri Azure Sx Updater");
      return;
    }
    while (!isPrimed()) {
      startTime = Instant.now();

      URI uri = new URIBuilder(url)
        .addParameter("publishFromDateTime", fromDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
        .addParameter("publishToDateTime", toDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
        .build();

      LOG.info("Fetching initial Siri SX data from {}, timeout is {}ms", uri, timeout);

      final long t1 = System.currentTimeMillis();
      String string = fetchInitialData(uri);
      final long t2 = System.currentTimeMillis();

      LOG.info(
        "Fetching initial data - finished after {} ms, got {} bytes",
        (t2 - t1),
        string.length()
      );

      // This is fine since runnables are scheduled after each other
      processHistory(string, "SX-INITIAL-1");
    }
  }

  private Siri getSiri(String message, String id) throws XMLStreamException, JAXBException {
    var siri = SiriXml.parseXml(message);
    if (
      siri.getServiceDelivery() == null ||
      siri.getServiceDelivery().getSituationExchangeDeliveries() == null ||
      siri.getServiceDelivery().getSituationExchangeDeliveries().isEmpty()
    ) {
      if (siri.getHeartbeatNotification() != null) {
        LOG.debug("Received SIRI heartbeat message");
      } else {
        LOG.warn("Empty Siri message for messageId {}", id);
        LOG.debug(message);
      }
      return null;
    }
    return siri;
  }

  private void processMessage(String message, String id) {
    try {
      Siri siri = getSiri(message, id);
      if (siri == null) {
        return;
      }

      super.saveResultOnGraph.execute((graph, transitModel) ->
        updateHandler.update(siri.getServiceDelivery())
      );
    } catch (JAXBException | XMLStreamException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

  private void processHistory(String message, String id) {
    try {
      Siri siri = getSiri(message, id);
      if (siri == null) {
        LOG.info("Did not receive any SX messages from history endpoint.");
        return;
      }

      var f = super.saveResultOnGraph.execute((graph, transitModel) -> {
        try {
          long t1 = System.currentTimeMillis();
          updateHandler.update(siri.getServiceDelivery());

          LOG.info(
            "Azure SX updater initialized after {} ms: [time since startup: {}]",
            (System.currentTimeMillis() - t1),
            DurationUtils.durationToStr(Duration.between(startTime, Instant.now()))
          );
          setPrimed(true);
        } catch (Exception e) {
          LOG.error("Could not process SX history", e);
        }
      });
      f.get();
    } catch (JAXBException | XMLStreamException | ExecutionException | InterruptedException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

  @Override
  public TransitAlertService getTransitAlertService() {
    return this.transitAlertService;
  }
}
