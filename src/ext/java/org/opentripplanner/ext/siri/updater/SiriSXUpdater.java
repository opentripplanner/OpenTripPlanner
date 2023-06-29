package org.opentripplanner.ext.siri.updater;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.opentripplanner.ext.siri.SiriAlertsUpdateHandler;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.ext.siri.SiriHttpUtils;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.alert.TransitAlertProvider;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ServiceDelivery;
import uk.org.siri.siri20.Siri;

public class SiriSXUpdater extends PollingGraphUpdater implements TransitAlertProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SiriSXUpdater.class);
  private static final long RETRY_INTERVAL_MILLIS = 5000;
  private final String url;
  private final String originalRequestorRef;
  private final TransitAlertService transitAlertService;
  private final SiriAlertsUpdateHandler updateHandler;
  private final HttpHeaders requestHeaders;
  private WriteToGraphCallback saveResultOnGraph;
  private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusWeeks(1);
  private String requestorRef;
  private int timeoutMillis = 0;
  private int retryCount = 0;

  public SiriSXUpdater(SiriSXUpdaterParameters config, TransitModel transitModel) {
    super(config);
    // TODO: add options to choose different patch services
    this.url = config.url();
    this.requestorRef = config.requestorRef();

    if (requestorRef == null || requestorRef.isEmpty()) {
      requestorRef = "otp-" + UUID.randomUUID().toString();
    }

    //Keeping original requestorRef use as base for updated requestorRef to be used in retries
    this.originalRequestorRef = requestorRef;
    this.timeoutMillis = DurationUtils.toIntMilliseconds(config.timeout(), 0);
    this.blockReadinessUntilInitialized = config.blockReadinessUntilInitialized();
    this.requestHeaders = config.requestHeaders();
    this.transitAlertService = new TransitAlertServiceImpl(transitModel);
    this.updateHandler =
      new SiriAlertsUpdateHandler(
        config.feedId(),
        transitModel,
        transitAlertService,
        SiriFuzzyTripMatcher.of(new DefaultTransitService(transitModel)),
        config.earlyStart()
      );
    LOG.info(
      "Creating real-time alert updater (SIRI SX) running every {} seconds : {}",
      pollingPeriod(),
      url
    );
  }

  @Override
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
  }

  public TransitAlertService getTransitAlertService() {
    return transitAlertService;
  }

  public String toString() {
    return "SiriSXUpdater (" + url + ")";
  }

  @Override
  protected void runPolling() throws InterruptedException {
    try {
      boolean moreData = false;
      do {
        Siri updates = getUpdates();
        if (updates != null) {
          ServiceDelivery serviceDelivery = updates.getServiceDelivery();
          moreData = Boolean.TRUE.equals(serviceDelivery.isMoreData());
          // Mark this updater as primed after last page of updates. Copy moreData into a final
          // primitive, because the object moreData persists across iterations.
          final boolean markPrimed = !moreData;
          if (serviceDelivery.getSituationExchangeDeliveries() != null) {
            saveResultOnGraph.execute((graph, transitModel) -> {
              updateHandler.update(serviceDelivery);
              if (markPrimed) primed = true;
            });
          }
        }
      } while (moreData);
    } catch (IOException e) {
      final long sleepTime = RETRY_INTERVAL_MILLIS + RETRY_INTERVAL_MILLIS * retryCount;

      retryCount++;

      LOG.info("Caught timeout - retry no. {} after {} millis", retryCount, sleepTime);

      Thread.sleep(sleepTime);

      // Creating new requestorRef so all data is refreshed
      requestorRef = originalRequestorRef + "-retry-" + retryCount;
      runPolling();
    }
  }

  private Siri getUpdates() throws IOException {
    long t1 = System.currentTimeMillis();
    long creating = 0;
    long fetching = 0;
    long unmarshalling = 0;
    try {
      String sxServiceRequest = SiriHelper.createSXServiceRequestAsXml(requestorRef);
      creating = System.currentTimeMillis() - t1;
      t1 = System.currentTimeMillis();

      InputStream is = SiriHttpUtils.postData(
        url,
        sxServiceRequest,
        timeoutMillis,
        requestHeaders.asMap()
      );

      fetching = System.currentTimeMillis() - t1;
      t1 = System.currentTimeMillis();

      Siri siri = SiriHelper.unmarshal(is);

      unmarshalling = System.currentTimeMillis() - t1;
      if (siri == null) {
        throw new RuntimeException("Failed to get data from url " + url);
      }
      ServiceDelivery serviceDelivery = siri.getServiceDelivery();
      if (serviceDelivery == null) {
        throw new RuntimeException("Failed to get serviceDelivery " + url);
      }

      ZonedDateTime responseTimestamp = serviceDelivery.getResponseTimestamp();
      if (responseTimestamp.isBefore(lastTimestamp)) {
        LOG.info("Ignoring feed with an old timestamp.");
        return null;
      }

      lastTimestamp = responseTimestamp;
      return siri;
    } catch (IOException e) {
      LOG.info("Failed after {} ms", (System.currentTimeMillis() - t1));
      LOG.error("Error reading SIRI feed from " + url, e);
      throw e;
    } catch (Exception e) {
      LOG.info("Failed after {} ms", (System.currentTimeMillis() - t1));
      LOG.error("Error reading SIRI feed from " + url, e);
    } finally {
      LOG.info(
        "Updating SX [{}]: Create req: {}, Fetching data: {}, Unmarshalling: {}",
        requestorRef,
        creating,
        fetching,
        unmarshalling
      );
    }
    return null;
  }
}
