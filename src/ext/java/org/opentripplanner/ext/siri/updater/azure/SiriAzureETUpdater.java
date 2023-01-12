package org.opentripplanner.ext.siri.updater.azure;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.google.common.io.CharStreams;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.client.utils.URIBuilder;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.trip.UpdateResult;
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

  private long startTime;

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
      LOG.info("Total SIRI-ET messages received={}", MESSAGE_COUNTER.get());
    }

    processMessage(message.getBody().toString(), message.getMessageId());
  }

  @Override
  protected void errorConsumer(ServiceBusErrorContext errorContext) {
    defaultErrorConsumer(errorContext);
  }

  @Override
  protected void initializeData(String url, Consumer<ServiceBusReceivedMessageContext> consumer)
    throws IOException, URISyntaxException {
    if (url == null) {
      LOG.info("No history url set up for Siri Azure ET Updater");
      return;
    }

    URI uri = new URIBuilder(url)
      .addParameter("fromDateTime", fromDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
      .build();

    startTime = now();
    LOG.info("Fetching initial Siri ET data from {}, timeout is {}ms", uri, timeout);

    HashMap<String, String> headers = new HashMap<>();
    headers.put("Accept", "application/xml");

    final long t1 = System.currentTimeMillis();
    final InputStream data = HttpUtils.getData(uri, Duration.ofMillis(timeout), headers);
    final long t2 = System.currentTimeMillis();

    if (data == null) {
      throw new IOException("Historical endpoint returned no data from url" + url);
    }

    var reader = new InputStreamReader(data);
    var string = CharStreams.toString(reader);

    LOG.info(
      "Fetching initial data - finished after {} ms, got {} bytes",
      (t2 - t1),
      string.length()
    );

    // This is fine since runnables are scheduled after each other
    processHistory(string, "ET-INITIAL-1");
  }

  private void processMessage(String message, String id) {
    try {
      List<EstimatedTimetableDeliveryStructure> updates = getUpdates(message, id);

      if (updates.isEmpty()) {
        return;
      }

      super.saveResultOnGraph.execute((graph, transitModel) ->
        snapshotSource.applyEstimatedTimetable(
          transitModel,
          fuzzyTripMatcher(),
          feedId,
          false,
          updates
        )
      );
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

      super.saveResultOnGraph.execute((graph, transitModel) -> {
        long t1 = System.currentTimeMillis();
        var result = snapshotSource.applyEstimatedTimetable(
          transitModel,
          fuzzyTripMatcher(),
          feedId,
          false,
          updates
        );
        recordMetrics.accept(result);

        setPrimed(true);
        LOG.info(
          "Azure ET updater initialized after {} ms: [time since startup: {}]",
          (System.currentTimeMillis() - t1),
          DurationFormatUtils.formatDuration((now() - startTime), "HH:mm:ss")
        );
      });
    } catch (JAXBException | XMLStreamException e) {
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
        LOG.info("Received SIRI heartbeat message");
      } else {
        LOG.warn("Empty Siri message {}: {}", id, message);
      }
      return new ArrayList<>();
    }

    return siri.getServiceDelivery().getEstimatedTimetableDeliveries();
  }
}
