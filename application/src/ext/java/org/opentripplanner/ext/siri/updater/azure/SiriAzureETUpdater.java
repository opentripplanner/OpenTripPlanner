package org.opentripplanner.ext.siri.updater.azure;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import jakarta.xml.bind.JAXBException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamException;
import org.apache.hc.core5.net.URIBuilder;
import org.opentripplanner.updater.siri.SiriRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.spi.ResultLogger;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.ServiceDelivery;

public class SiriAzureETUpdater extends AbstractAzureSiriUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(SiriAzureSXUpdater.class);

  private static final AtomicLong MESSAGE_COUNTER = new AtomicLong(0);

  private final LocalDate fromDateTime;
  private final SiriRealTimeTripUpdateAdapter adapter;

  private final Consumer<UpdateResult> recordMetrics;

  public SiriAzureETUpdater(
    SiriAzureETUpdaterParameters config,
    SiriRealTimeTripUpdateAdapter adapter
  ) {
    super(config);
    this.fromDateTime = config.getFromDateTime();
    this.adapter = adapter;
    this.recordMetrics = TripUpdateMetrics.streaming(config);
  }

  @Override
  protected void messageConsumer(ServiceBusReceivedMessageContext messageContext) {
    var message = messageContext.getMessage();
    MESSAGE_COUNTER.incrementAndGet();

    if (MESSAGE_COUNTER.get() % 100 == 0) {
      LOG.debug("Total SIRI-ET messages received={}", MESSAGE_COUNTER.get());
    }

    try {
      var updates = parseSiriEt(message.getBody().toString(), message.getMessageId());
      if (!updates.isEmpty()) {
        processMessage(updates);
      }
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
      LOG.info("No history url set up for Siri Azure ET Updater");
      return;
    }

    URI uri = new URIBuilder(url)
      .addParameter("fromDateTime", fromDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
      .build();

    LOG.info("Fetching initial Siri ET data from {}, timeout is {} ms.", uri, timeout);
    var siri = fetchInitialSiriData(uri);

    if (siri.isEmpty()) {
      LOG.info("Got empty ET response from history endpoint");
      return;
    }

    // This is fine since runnables are scheduled after each other
    processHistory(siri.get());
  }

  private Future<?> processMessage(List<EstimatedTimetableDeliveryStructure> updates) {
    return super.saveResultOnGraph.execute(context -> {
      var result = adapter.applyEstimatedTimetable(
        fuzzyTripMatching() ? context.siriFuzzyTripMatcher() : null,
        context.entityResolver(feedId),
        feedId,
        UpdateIncrementality.DIFFERENTIAL,
        updates
      );
      ResultLogger.logUpdateResultErrors(feedId, "siri-et", result);
      recordMetrics.accept(result);
    });
  }

  private void processHistory(ServiceDelivery siri) {
    var updates = siri.getEstimatedTimetableDeliveries();

    if (updates == null || updates.isEmpty()) {
      LOG.info("Did not receive any ET messages from history endpoint");
      return;
    }

    try {
      long t1 = System.currentTimeMillis();
      var f = processMessage(updates);
      f.get();
      LOG.info("Azure ET updater initialized in {} ms.", (System.currentTimeMillis() - t1));
    } catch (ExecutionException | InterruptedException e) {
      throw new SiriAzureInitializationException("Error applying history", e);
    }
  }

  private List<EstimatedTimetableDeliveryStructure> parseSiriEt(String siriXmlMessage, String id)
    throws JAXBException, XMLStreamException {
    var siri = SiriXml.parseXml(siriXmlMessage);
    if (
      siri.getServiceDelivery() == null ||
      siri.getServiceDelivery().getEstimatedTimetableDeliveries() == null ||
      siri.getServiceDelivery().getEstimatedTimetableDeliveries().isEmpty()
    ) {
      if (siri.getHeartbeatNotification() != null) {
        LOG.debug("Received SIRI heartbeat message");
      } else {
        LOG.info("Empty Siri message {}: {}", id, siriXmlMessage);
      }
      return new ArrayList<>();
    }

    return siri.getServiceDelivery().getEstimatedTimetableDeliveries();
  }
}
