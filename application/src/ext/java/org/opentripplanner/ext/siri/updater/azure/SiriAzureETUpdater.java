package org.opentripplanner.ext.siri.updater.azure;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import jakarta.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamException;
import org.opentripplanner.updater.siri.SiriRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.spi.ResultLogger;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.ServiceDelivery;

public class SiriAzureETUpdater implements SiriAzureMessageHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SiriAzureSXUpdater.class);

  private static final AtomicLong MESSAGE_COUNTER = new AtomicLong(0);

  private final SiriRealTimeTripUpdateAdapter adapter;
  private final Consumer<UpdateResult> recordMetrics;
  private final boolean fuzzyTripMatching;
  private final String feedId;

  private WriteToGraphCallback writeToGraphCallback;

  public SiriAzureETUpdater(
    SiriAzureETUpdaterParameters config,
    SiriRealTimeTripUpdateAdapter adapter
  ) {
    this.adapter = adapter;
    this.recordMetrics = TripUpdateMetrics.streaming(config);
    this.fuzzyTripMatching = config.isFuzzyTripMatching();
    this.feedId = Objects.requireNonNull(config.feedId(), "feedId must not be null");
  }

  @Override
  public void setup(WriteToGraphCallback writeToGraphCallback) {
    this.writeToGraphCallback = writeToGraphCallback;
  }

  @Override
  public void handleMessage(ServiceBusReceivedMessageContext messageContext) {
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

  private Future<?> processMessage(List<EstimatedTimetableDeliveryStructure> updates) {
    return writeToGraphCallback.execute(context -> {
      var result = adapter.applyEstimatedTimetable(
        fuzzyTripMatching ? context.siriFuzzyTripMatcher() : null,
        context.entityResolver(feedId),
        feedId,
        UpdateIncrementality.DIFFERENTIAL,
        updates
      );
      ResultLogger.logUpdateResultErrors(feedId, "siri-et", result);
      recordMetrics.accept(result);
    });
  }

  @Override
  public void processHistory(ServiceDelivery siri) {
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
