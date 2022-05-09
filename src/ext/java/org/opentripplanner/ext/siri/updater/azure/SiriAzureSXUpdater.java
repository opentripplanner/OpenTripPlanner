package org.opentripplanner.ext.siri.updater.azure;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.client.utils.URIBuilder;
import org.opentripplanner.ext.siri.SiriAlertsUpdateHandler;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.updater.alerts.TransitAlertProvider;
import org.opentripplanner.util.HttpUtils;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

public class SiriAzureSXUpdater extends AbstractAzureSiriUpdater implements TransitAlertProvider {

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private SiriAlertsUpdateHandler updateHandler;
  private TransitAlertService transitAlertService;

  private static final transient AtomicLong messageCounter = new AtomicLong(0);
  private final LocalDate fromDateTime;
  private final LocalDate toDateTime;
  private long startTime;

  public SiriAzureSXUpdater(SiriAzureSXUpdaterParameters config) {
    super(config);
    this.fromDateTime = config.getFromDateTime();
    this.toDateTime = config.getToDateTime();
  }

  @Override
  public void setup(Graph graph) throws Exception {
    super.setup(graph);
    this.transitAlertService = new TransitAlertServiceImpl(graph);
    SiriFuzzyTripMatcher fuzzyTripMatcher = new SiriFuzzyTripMatcher(new RoutingService(graph));
    if (updateHandler == null) {
      updateHandler = new SiriAlertsUpdateHandler(feedId, graph);
    }
    updateHandler.setTransitAlertService(transitAlertService);
    updateHandler.setSiriFuzzyTripMatcher(fuzzyTripMatcher);
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
    throws IOException, URISyntaxException {
    if (url == null) {
      LOG.info("No history url set up for Siri Azure Sx Updater");
      return;
    }
    startTime = now();

    URI uri = new URIBuilder(url)
      .addParameter("publishFromDateTime", fromDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
      .addParameter("publishToDateTime", toDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
      .build();

    LOG.info("Fetching initial Siri SX data from {}, timeout is {}ms", url, timeout);

    final long t1 = System.currentTimeMillis();
    HashMap<String, String> headers = new HashMap<>();
    headers.put("Accept", "application/xml");

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
    processHistory(string, "SX-INITIAL-1");
  }

  private Siri getSiri(String message, String id) throws JAXBException, XMLStreamException {
    var siri = SiriXml.parseXml(message);
    if (
      siri.getServiceDelivery() == null ||
      siri.getServiceDelivery().getSituationExchangeDeliveries() == null ||
      siri.getServiceDelivery().getSituationExchangeDeliveries().isEmpty()
    ) {
      LOG.warn("Empty Siri message for messageId {}", id);
      LOG.debug(message);
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

      super.saveResultOnGraph.execute(graph -> updateHandler.update(siri.getServiceDelivery()));
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

      super.saveResultOnGraph.execute(graph -> {
        long t1 = System.currentTimeMillis();
        updateHandler.update(siri.getServiceDelivery());

        setPrimed(true);
        LOG.info(
          "Azure SX updater initialized after {} ms: [time since startup: {}]",
          (System.currentTimeMillis() - t1),
          DurationFormatUtils.formatDuration((now() - startTime), "HH:mm:ss")
        );
      });
    } catch (JAXBException | XMLStreamException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

  @Override
  public TransitAlertService getTransitAlertService() {
    return this.transitAlertService;
  }
}
