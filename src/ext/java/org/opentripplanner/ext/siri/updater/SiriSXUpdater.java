package org.opentripplanner.ext.siri.updater;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import org.opentripplanner.ext.siri.SiriAlertsUpdateHandler;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.framework.retry.OtpRetry;
import org.opentripplanner.framework.retry.OtpRetryBuilder;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.alert.TransitAlertProvider;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ServiceDelivery;
import uk.org.siri.siri20.Siri;

public class SiriSXUpdater extends PollingGraphUpdater implements TransitAlertProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SiriSXUpdater.class);
  private static final int RETRY_MAX_ATTEMPTS = 3;
  private static final Duration RETRY_INITIAL_DELAY = Duration.ofSeconds(5);
  private static final int RETRY_BACKOFF = 2;

  private final String url;
  private final String originalRequestorRef;
  private final TransitAlertService transitAlertService;
  private final SiriAlertsUpdateHandler updateHandler;
  private WriteToGraphCallback saveResultOnGraph;
  private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusWeeks(1);
  private String requestorRef;
  /**
   * Global retry counter used to create a new unique requestorRef after each retry.
   */
  private int retryCount = 0;
  private final SiriHttpLoader siriHttpLoader;
  private final OtpRetry retry;

  public SiriSXUpdater(SiriSXUpdaterParameters config, TransitModel transitModel) {
    super(config);
    // TODO: add options to choose different patch services
    this.url = config.url();
    this.requestorRef = config.requestorRef();

    if (requestorRef == null || requestorRef.isEmpty()) {
      requestorRef = "otp-" + UUID.randomUUID();
    }

    //Keeping original requestorRef use as base for updated requestorRef to be used in retries
    this.originalRequestorRef = requestorRef;
    this.blockReadinessUntilInitialized = config.blockReadinessUntilInitialized();
    this.transitAlertService = new TransitAlertServiceImpl(transitModel);
    this.updateHandler =
      new SiriAlertsUpdateHandler(
        config.feedId(),
        transitModel,
        transitAlertService,
        SiriFuzzyTripMatcher.of(new DefaultTransitService(transitModel)),
        config.earlyStart()
      );
    siriHttpLoader = new SiriHttpLoader(url, config.timeout(), config.requestHeaders());

    retry =
      new OtpRetryBuilder()
        .withName("SIRI-SX Update")
        .withMaxAttempts(RETRY_MAX_ATTEMPTS)
        .withInitialRetryInterval(RETRY_INITIAL_DELAY)
        .withBackoffMultiplier(RETRY_BACKOFF)
        .withRetryableException(OtpHttpClientException.class::isInstance)
        .withOnRetry(this::updateRequestorRef)
        .build();

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
    retry.execute(this::updateSiri);
  }

  private void updateSiri() {
    boolean moreData = false;
    do {
      var updates = getUpdates();
      if (updates.isPresent()) {
        ServiceDelivery serviceDelivery = updates.get().getServiceDelivery();
        moreData = Boolean.TRUE.equals(serviceDelivery.isMoreData());
        // Mark this updater as primed after last page of updates. Copy moreData into a final
        // primitive, because the object moreData persists across iterations.
        final boolean markPrimed = !moreData;
        if (serviceDelivery.getSituationExchangeDeliveries() != null) {
          saveResultOnGraph.execute((graph, transitModel) -> {
            updateHandler.update(serviceDelivery);
            if (markPrimed) {
              primed = true;
            }
          });
        }
      }
    } while (moreData);
  }

  private Optional<Siri> getUpdates() {
    long t1 = System.currentTimeMillis();
    try {
      Optional<Siri> siri = siriHttpLoader.fetchSXFeed(requestorRef);
      if (siri.isEmpty()) {
        return Optional.empty();
      }

      ServiceDelivery serviceDelivery = siri.get().getServiceDelivery();
      if (serviceDelivery == null) {
        throw new RuntimeException("Failed to get serviceDelivery " + url);
      }

      ZonedDateTime responseTimestamp = serviceDelivery.getResponseTimestamp();
      if (responseTimestamp.isBefore(lastTimestamp)) {
        LOG.info("Ignoring feed with an old timestamp.");
        return Optional.empty();
      }

      lastTimestamp = responseTimestamp;
      return siri;
    } catch (OtpHttpClientException e) {
      LOG.info(
        "Retryable exception while reading SIRI feed from {} after {} ms",
        url,
        (System.currentTimeMillis() - t1)
      );
      throw e;
    } catch (Exception e) {
      LOG.error(
        "Non-retryable exception while reading SIRI feed from {} after {} ms",
        url,
        (System.currentTimeMillis() - t1)
      );
    }
    return Optional.empty();
  }

  /**
   * Reset the session with the SIRI-SX server by creating a new unique requestorRef. This is
   * required if a network error causes a request to fail and let the session in an undetermined
   * state. Using a new requestorRef will force the SIRI-SX server to send again all available
   * messages.
   */
  private void updateRequestorRef() {
    retryCount++;
    requestorRef = originalRequestorRef + "-retry-" + retryCount;
  }
}
