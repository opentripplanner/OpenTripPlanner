package org.opentripplanner.updater.alert.siri;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.framework.retry.OtpRetry;
import org.opentripplanner.framework.retry.OtpRetryBuilder;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.alert.TransitAlertProvider;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.support.siri.SiriLoader;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.ServiceDelivery;
import uk.org.siri.siri21.Siri;

public class SiriSXUpdater extends PollingGraphUpdater implements TransitAlertProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SiriSXUpdater.class);
  private static final int RETRY_MAX_ATTEMPTS = 3;
  private static final Duration RETRY_INITIAL_DELAY = Duration.ofSeconds(5);
  private static final int RETRY_BACKOFF = 2;

  private final String url;
  private final String originalRequestorRef;
  private final TransitAlertService transitAlertService;

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
    TimetableRepository timetableRepository,
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
    this.transitAlertService = new TransitAlertServiceImpl(timetableRepository);
    this.updateHandler = new SiriAlertsUpdateHandler(
      config.feedId(),
      transitAlertService,
      config.earlyStart()
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

  public TransitAlertService getTransitAlertService() {
    return transitAlertService;
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
    do {
      var updates = getUpdates();
      if (updates.isPresent()) {
        ServiceDelivery serviceDelivery = updates.get().getServiceDelivery();
        moreData = Boolean.TRUE.equals(serviceDelivery.isMoreData());
        // Mark this updater as primed after last page of updates. Copy moreData into a final
        // primitive, because the object moreData persists across iterations.
        final boolean markPrimed = !moreData;
        if (serviceDelivery.getSituationExchangeDeliveries() != null) {
          // FIXME RT_AB: This is submitting a reference to a method on a long-lived instance as a
          //   GraphWriterRunnable. These runnables were originally intended to be small,
          //   self-contained, throw-away update tasks.
          //   See org/opentripplanner/updater/trip/PollingTripUpdater.java:90
          //   Clarify why the long-lived instance is capturing and holding so many references.
          //   The runnable should only contain the minimum needed to operate on the graph.
          //   Such runnables should be illustrated in documentation as e.g. a little box labeled
          //   "change trip ABC123 by making stop 53 late by 2 minutes."
          //   Also clarify how this runnable works without even using the supplied
          //   (graph, timetableRepository) parameters. There are multiple TransitAlertServices and they
          //   are not versioned along with the Graph, they are attached to updaters.
          //
          // This is submitting a runnable to an executor, but that runnable only writes back to
          // objects referenced by updateHandler itself, rather than the graph or timetableRepository
          // supplied for writing, and apparently with no versioning. This seems like a
          // misinterpretation of the realtime design.
          // If this is an intentional choice to live-patch a single server-wide instance of an
          // alerts service/index while it's already in use by routing, we should be clear about
          // this and document why it differs from the graph-writer design. Currently the code
          // seems to follow some surface conventions of the threadsafe copy-on-write pattern
          // without actually providing threadsafe behavior.
          // It's a reasonable choice to defer processing the list of alerts to another thread than
          // this fetching thread, but we probably don't want to defer any such processing to the
          // graph writer thread, as that's explicitly restricted to be one single shared thread for
          // the entire application. There seems to be a misunderstanding that the tasks are
          // submitted to get them off the updater thread, but the real reason is to ensure
          // consistent transactions in graph writing and reading.
          // All that said, out of all the update types, Alerts (and SIRI SX) are probably the ones
          // that would be most tolerant of non-versioned application-wide storage since they don't
          // participate in routing and are tacked on to already-completed routing responses.

          updateGraph(context -> {
            updateHandler.update(serviceDelivery, context);
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

  public interface Parameters extends PollingGraphUpdaterParameters, UrlUpdaterParameters {
    String requestorRef();

    boolean blockReadinessUntilInitialized();

    Duration earlyStart();
  }
}
