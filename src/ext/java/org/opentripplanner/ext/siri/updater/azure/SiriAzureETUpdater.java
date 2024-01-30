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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamException;
import org.apache.hc.core5.net.URIBuilder;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.spi.ResultLogger;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

public class SiriAzureETUpdater extends AbstractAzureSiriUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(SiriAzureSXUpdater.class);

  private static final AtomicLong MESSAGE_COUNTER = new AtomicLong(0);

  private final LocalDate fromDateTime;
  private final SiriTimetableSnapshotSource snapshotSource;

  private Instant startTime;

  private final Consumer<UpdateResult> recordMetrics;

  public SiriAzureETUpdater(
    SiriAzureETUpdaterParameters config,
    TransitModel transitModel,
    SiriTimetableSnapshotSource snapshotSource
  ) {
    super(config, transitModel);
    this.fromDateTime = config.getFromDateTime();
    this.snapshotSource = snapshotSource;
    this.recordMetrics = TripUpdateMetrics.streaming(config);
  }

  @Override
  protected void messageConsumer(ServiceBusReceivedMessageContext messageContext) {
    var message = messageContext.getMessage();
    MESSAGE_COUNTER.incrementAndGet();

    if (MESSAGE_COUNTER.get() % 100 == 0) {
      LOG.debug("Total SIRI-ET messages received={}", MESSAGE_COUNTER.get());
    }

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
      LOG.info("No history url set up for Siri Azure ET Updater");
      return;
    }

    URI uri = new URIBuilder(url)
      .addParameter("fromDateTime", fromDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
      .build();

    while (!isPrimed()) {
      startTime = Instant.now();
      LOG.info("Fetching initial Siri ET data from {}, timeout is {}ms", uri, timeout);
      final long t1 = System.currentTimeMillis();
      String string = fetchInitialData(uri);
      final long t2 = System.currentTimeMillis();

      LOG.info(
        "Fetching initial data - finished after {} ms, got {} bytes",
        (t2 - t1),
        string.length()
      );

      // This is fine since runnables are scheduled after each other
      processHistory(string, "ET-INITIAL-1");
    }
  }

  private void processMessage(String message, String id) {
    try {
      List<EstimatedTimetableDeliveryStructure> updates = getUpdates(message, id);

      if (updates.isEmpty()) {
        return;
      }

      super.saveResultOnGraph.execute((graph, transitModel) -> {
        var result = snapshotSource.applyEstimatedTimetable(
          fuzzyTripMatcher(),
          entityResolver(),
          feedId,
          false,
          updates
        );
        ResultLogger.logUpdateResultErrors(feedId, "siri-et", result);
        recordMetrics.accept(result);
      });
    } catch (JAXBException | XMLStreamException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

  private void processHistory(String message, String id) {
    try {
      List<EstimatedTimetableDeliveryStructure> updates = getUpdates(message, id);

      if (updates.isEmpty()) {
        LOG.info("Did not receive any ET messages from history endpoint");
        return;
      }

      var f = super.saveResultOnGraph.execute((graph, transitModel) -> {
        try {
          long t1 = System.currentTimeMillis();
          var result = snapshotSource.applyEstimatedTimetable(
            fuzzyTripMatcher(),
            entityResolver(),
            feedId,
            false,
            updates
          );
          ResultLogger.logUpdateResultErrors(feedId, "siri-et", result);
          recordMetrics.accept(result);

          setPrimed(true);
          LOG.info(
            "Azure ET updater initialized after {} ms: [time since startup: {}]",
            (System.currentTimeMillis() - t1),
            DurationUtils.durationToStr(Duration.between(startTime, Instant.now()))
          );
        } catch (Exception e) {
          LOG.error("Could not process ET history", e);
        }
      });
      f.get();
    } catch (JAXBException | XMLStreamException | ExecutionException | InterruptedException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

  private List<EstimatedTimetableDeliveryStructure> getUpdates(String message, String id)
    throws JAXBException, XMLStreamException {
    var siri = SiriXml.parseXml(message);
    if (
      siri.getServiceDelivery() == null ||
      siri.getServiceDelivery().getEstimatedTimetableDeliveries() == null ||
      siri.getServiceDelivery().getEstimatedTimetableDeliveries().isEmpty()
    ) {
      if (siri.getHeartbeatNotification() != null) {
        LOG.debug("Received SIRI heartbeat message");
      } else {
        LOG.warn("Empty Siri message {}: {}", id, message);
      }
      return new ArrayList<>();
    }

    return siri.getServiceDelivery().getEstimatedTimetableDeliveries();
  }
}
