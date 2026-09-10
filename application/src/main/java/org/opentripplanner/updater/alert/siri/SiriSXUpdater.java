package org.opentripplanner.updater.alert.siri;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.framework.retry.OtpRetry;
import org.opentripplanner.framework.retry.OtpRetryBuilder;
import org.opentripplanner.updater.AlertRealTimeUpdateContext;
import org.opentripplanner.updater.UpdateIncrementality;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.spi.WriteDomain;
import org.opentripplanner.updater.support.siri.SiriLoader;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatcherCache;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.ServiceDelivery;
import uk.org.siri.siri21.Siri;

public class SiriSXUpdater extends PollingGraphUpdater<AlertRealTimeUpdateContext> {

  private static final Logger LOG = LoggerFactory.getLogger(SiriSXUpdater.class);
  private static final int RETRY_MAX_ATTEMPTS = 3;
  private static final Duration RETRY_INITIAL_DELAY = Duration.ofSeconds(5);
  private static final int RETRY_BACKOFF = 2;

  private final String url;
  private final String originalRequestorRef;

  // TODO RT_AB: Document why SiriAlertsUpdateHandler is a separate instance that persists across
  //  many graph update operations.
  private final SiriAlertsUpdateHandler updateHandler;
  private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusWeeks(1);
  private String requestorRef;
  /**
   * Global retry counter used to create a new unique requestorRef after each retry.
   */
  private int retryCount = 0;
  private final SiriLoader siriHttpLoader;
  private final OtpRetry retry;

  public SiriSXUpdater(
    Parameters config,
    @Nullable SiriFuzzyTripMatcherCache siriFuzzyTripMatcherCache,
    SiriLoader siriLoader
  ) {
    super(config);
    this.url = config.url();
    this.requestorRef = config.requestorRef();

    if (requestorRef == null || requestorRef.isEmpty()) {
      requestorRef = "otp-" + UUID.randomUUID();
    }

    //Keeping original requestorRef use as base for updated requestorRef to be used in retries
    this.originalRequestorRef = requestorRef;
    this.blockReadinessUntilInitialized = config.blockReadinessUntilInitialized();
    this.updateHandler = new SiriAlertsUpdateHandler(
      config.feedId(),
      config.earlyStart(),
      siriFuzzyTripMatcherCache
    );
    siriHttpLoader = siriLoader;

    retry = new OtpRetryBuilder()
      .withName("SIRI-SX Update")
      .withMaxAttempts(RETRY_MAX_ATTEMPTS)
      .withInitialRetryInterval(RETRY_INITIAL_DELAY)
      .withBackoffMultiplier(RETRY_BACKOFF)
      .withRetryableException(OtpHttpClientException.class::isInstance)
      .withOnRetry(this::updateRequestorRef)
      .build();

    LOG.info("Creating SIRI-SX updater running every {}: {}", pollingPeriod(), url);
  }

  @Override
  public WriteDomain<AlertRealTimeUpdateContext> writeDomain() {
    return WriteDomain.ALERT;
  }

  @Override
  protected void runPolling() throws InterruptedException {
    retry.execute(this::updateSiri);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(SiriSXUpdater.class)
      .addStr("url", url)
      .addDuration("frequency", pollingPeriod())
      .toString();
  }

  /**
   * This part of the update process has been factored out to allow repeated retries of the HTTP
   * fetching operation in case the connection fails or some other disruption happens.
   */
  private void updateSiri() {
    boolean moreData = false;
    boolean firstPage = true;
    do {
      var updates = getUpdates();
      if (updates.isPresent()) {
        ServiceDelivery serviceDelivery = updates.get().getServiceDelivery();
        moreData = Boolean.TRUE.equals(serviceDelivery.isMoreData());
        // Mark this updater as primed after last page of updates. Copy moreData into a final
        // primitive, because the object moreData persists across iterations.
        final boolean markPrimed = !moreData;
        // Only the first page of a full data set may replace the previously stored alerts; the
        // remaining pages have to be merged into it.
        var incrementality = firstPage
          ? siriHttpLoader.incrementality()
          : UpdateIncrementality.DIFFERENTIAL;
        firstPage = false;
        if (serviceDelivery.getSituationExchangeDeliveries() != null) {
          var task = updateGraph(context -> {
            updateHandler.update(serviceDelivery, incrementality, context);
            if (markPrimed) {
              primed = true;
            }
          });
          if (!awaitAppliedOrResync(task)) {
            return;
          }
        }
      }
    } while (moreData);
  }

  /**
   * Wait for the write task to be applied and committed.
   * <p>
   * The alert domain commits atomically, so a task that throws is rolled back: nothing of this
   * delivery was stored. The SIRI-SX server only sends the situations that changed since the
   * previous request with the same requestorRef, so the rolled-back situations would be lost for
   * good. Resetting the requestorRef makes the server send all available messages again on the
   * next poll.
   *
   * @return {@code true} if the delivery was applied, {@code false} if it was rolled back and the
   *         remaining pages of this polling cycle should be abandoned.
   */
  private boolean awaitAppliedOrResync(Future<?> task) {
    try {
      task.get();
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } catch (ExecutionException e) {
      LOG.warn(
        "Applying the SIRI-SX delivery from {} failed and was rolled back. Resetting the " +
          "requestorRef to re-fetch all situations on the next poll.",
        url,
        e.getCause()
      );
      updateRequestorRef();
      return false;
    }
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
        System.currentTimeMillis() - t1
      );
      throw e;
    } catch (Exception e) {
      LOG.error(
        "Non-retryable exception while reading SIRI feed from {} after {} ms",
        url,
        System.currentTimeMillis() - t1
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

  public interface Parameters extends PollingGraphUpdaterParameters, UrlUpdaterParameters {
    String requestorRef();

    boolean blockReadinessUntilInitialized();

    Duration earlyStart();
  }
}
