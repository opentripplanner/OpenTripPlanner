package org.opentripplanner.ext.siri.updater.azure;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusFailureReason;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.CreateSubscriptionOptions;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.xml.bind.JAXBException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import javax.xml.stream.XMLStreamException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.entur.siri21.util.SiriXml;
import org.opentripplanner.framework.application.ApplicationShutdownSupport;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.framework.retry.OtpRetry;
import org.opentripplanner.framework.retry.OtpRetryBuilder;
import org.opentripplanner.framework.retry.OtpRetryException;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.alert.TransitAlertProvider;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.siri.SiriRealTimeTripUpdateAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.ServiceDelivery;
import uk.org.siri.siri21.Siri;

/**
 * This is the main handler for siri messages over azure. It handles the generic code for
 * communicating with the azure service bus and delegates to SiriAzureETUpdater and
 * SiriAzureSXUpdater for ET and SX specific stuff.
 */
public class SiriAzureUpdater implements GraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(SiriAzureUpdater.class);
  private final String updaterType;
  private final AuthenticationType authenticationType;
  private final String fullyQualifiedNamespace;
  private final String configRef;
  private final String serviceBusUrl;
  private final String topicName;
  private final Duration autoDeleteOnIdle;
  private final int prefetchCount;

  private ServiceBusProcessorClient eventProcessor;
  private ServiceBusAdministrationClient serviceBusAdmin;
  private boolean isPrimed = false;
  private String subscriptionName;

  private static final AtomicLong MESSAGE_COUNTER = new AtomicLong(0);
  private static final int BACKOFF_MULTIPLIER = 2;
  private static final Duration INITIAL_RETRY_INTERVALS = Duration.ofSeconds(5);
  private static final int MESSAGE_COUNTER_LOG_INTERVAL = 100;
  private static final int ERROR_RETRY_WAIT_SECONDS = 5;
  private static final int MAX_ATTEMPTS = 5;

  protected final SiriAzureMessageHandler messageHandler;

  /**
   * The URL used to fetch all initial updates, null means don't fetch initial data
   */
  @Nullable
  private final URI dataInitializationUrl;

  /**
   * The timeout used when fetching historical data
   */
  private final int timeout;

  SiriAzureUpdater(SiriAzureUpdaterParameters config, SiriAzureMessageHandler messageHandler) {
    this.messageHandler = Objects.requireNonNull(messageHandler);

    try {
      this.dataInitializationUrl = config.buildDataInitializationUrl().orElse(null);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid history url", e);
    }

    this.configRef = Objects.requireNonNull(config.configRef(), "configRef must not be null");
    this.authenticationType = Objects.requireNonNull(
      config.getAuthenticationType(),
      "authenticationType must not be null"
    );
    this.topicName = Objects.requireNonNull(config.getTopicName(), "topicName must not be null");
    this.updaterType = Objects.requireNonNull(config.getType(), "type must not be null");
    this.timeout = config.getTimeout();
    this.autoDeleteOnIdle = config.getAutoDeleteOnIdle();
    this.prefetchCount = config.getPrefetchCount();

    if (authenticationType == AuthenticationType.FederatedIdentity) {
      this.fullyQualifiedNamespace = Objects.requireNonNull(
        config.getFullyQualifiedNamespace(),
        "fullyQualifiedNamespace must not be null when using FederatedIdentity authentication"
      );
      this.serviceBusUrl = null;
    } else if (authenticationType == AuthenticationType.SharedAccessKey) {
      this.serviceBusUrl = Objects.requireNonNull(
        config.getServiceBusUrl(),
        "serviceBusUrl must not be null when using SharedAccessKey authentication"
      );
      this.fullyQualifiedNamespace = null;
    } else {
      throw new IllegalArgumentException("Unsupported authentication type: " + authenticationType);
    }
  }

  public static SiriAzureUpdater createETUpdater(
    SiriAzureETUpdaterParameters config,
    SiriRealTimeTripUpdateAdapter adapter
  ) {
    var messageHandler = new SiriAzureETUpdater(config, adapter);
    return new SiriAzureUpdater(config, messageHandler);
  }

  public static SiriAzureUpdater createSXUpdater(
    SiriAzureSXUpdaterParameters config,
    TimetableRepository timetableRepository
  ) {
    var messageHandler = new SiriAzureSXUpdater(config, timetableRepository);
    return new SxWrapper(config, messageHandler);
  }

  /**
   * This wrapper class is a SiriAzureUpdater that implements the TransitAlertProvider interface so
   * it can be registered to handle SX messages. It delegates the actual SIRI-SX message processing
   * to the contained SiriAzureSXUpdater.
   */
  public static class SxWrapper extends SiriAzureUpdater implements TransitAlertProvider {

    SxWrapper(SiriAzureUpdaterParameters config, SiriAzureSXUpdater messageHandler) {
      super(config, messageHandler);
    }

    /**
     * Implements the TransitAlertProvider interface to allow this updater to be detected as a
     * source of transit alerts. This method delegates to the internal SiriAzureSXUpdater
     *
     * @return TransitAlertService from the SiriAzureSXUpdater
     */
    @Override
    public TransitAlertService getTransitAlertService() {
      return ((SiriAzureSXUpdater) messageHandler).getTransitAlertService();
    }
  }

  @Override
  public void setup(WriteToGraphCallback writeToGraphCallback) {
    this.messageHandler.setup(writeToGraphCallback);
  }

  @Override
  public void run() {
    try {
      // In Kubernetes this should be the POD identifier
      subscriptionName = System.getenv("HOSTNAME");
      if (subscriptionName == null || subscriptionName.isBlank()) {
        subscriptionName = "otp-" + UUID.randomUUID();
      }

      // Try each startup step with timeout, continue on failure for graceful degradation
      boolean isSuccess = executeStartupStep(
        this::setupSubscription,
        updaterType + ":ServiceBusSubscription"
      );

      executeStartupStep(
        () -> {
          var initialData = fetchInitialSiriData();
          if (initialData.isEmpty()) {
            LOG.info("Got empty response from history endpoint");
          } else {
            processInitialSiriData(initialData.get());
          }
        },
        updaterType + ":HistoricalSiriData"
      );

      if (isSuccess) {
        executeStartupStep(this::startEventProcessor, updaterType + ":ServiceBusEventProcessor");
      }

      // Set primed so OTP can start
      setPrimed();

      // Register shutdown hook only once, and only after subscriptionName is set
      registerShutdownHook();
    } catch (InterruptedException e) {
      LOG.info("Startup interrupted, aborting updater initialization");
      // Preserve interrupt status
      Thread.currentThread().interrupt();
      // Don't set primed, don't register shutdown hook - just exit
    }
  }

  /**
   * Attempts to execute a startup step with timeout. Logs errors but continues execution for
   * graceful degradation. Rethrows InterruptedException to abort startup process.
   */
  private boolean executeStartupStep(Runnable task, String stepDescription)
    throws InterruptedException {
    OtpRetry otpRetry = createOtpRetry(stepDescription);
    try {
      otpRetry.execute(task);
      LOG.info("{} completed successfully", stepDescription);
      return true;
    } catch (OtpRetryException e) {
      String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      LOG.warn(
        "REALTIME_STARTUP_FAILED_ALERT component={} status=FAILED error={}",
        stepDescription,
        message
      );
      return false;
    }
  }

  private void registerShutdownHook() {
    ApplicationShutdownSupport.addShutdownHook(
      "azure-siri-updater-shutdown-" + updaterType,
      this::performShutdown
    );
  }

  /**
   * Performs orderly shutdown of all resources with proper error handling.
   */
  private void performShutdown() {
    LOG.info("Starting shutdown for {} updater", updaterType);

    // 1. Close event processor
    if (eventProcessor != null) {
      try {
        eventProcessor.close();
        LOG.debug("Event processor closed successfully");
      } catch (Exception e) {
        LOG.warn("Error closing event processor: {}", e.getMessage());
      }
    }

    // 2. Delete subscription if we have admin client and subscription name
    if (serviceBusAdmin != null && subscriptionName != null) {
      try {
        serviceBusAdmin.deleteSubscription(topicName, subscriptionName);
        LOG.info("Subscription '{}' deleted on topic '{}'", subscriptionName, topicName);
      } catch (Exception e) {
        LOG.warn("Error deleting subscription '{}': {}", subscriptionName, e.getMessage());
      }
    }

    LOG.info("Shutdown complete for {} updater", updaterType);
  }

  /**
   * Sets up the Service Bus subscription, including checking old subscription, deleting if
   * necessary, and creating a new subscription.
   */
  private void setupSubscription() throws ServiceBusException {
    // Client with permissions to create subscription
    if (authenticationType == AuthenticationType.FederatedIdentity) {
      serviceBusAdmin = new ServiceBusAdministrationClientBuilder()
        .credential(
          fullyQualifiedNamespace,
          new DefaultAzureCredentialBuilder()
            // We use the current thread for fetching credentials since the default executor
            // service can't be used in the shutdownHook where we want to delete the subscription
            .executorService(MoreExecutors.newDirectExecutorService())
            .build()
        )
        .buildClient();
    } else if (authenticationType == AuthenticationType.SharedAccessKey) {
      serviceBusAdmin = new ServiceBusAdministrationClientBuilder()
        .connectionString(serviceBusUrl)
        .buildClient();
    }

    // Set options
    CreateSubscriptionOptions options = new CreateSubscriptionOptions().setAutoDeleteOnIdle(
      autoDeleteOnIdle
    );

    // Make sure there is no old subscription on serviceBus
    if (serviceBusAdmin.getSubscriptionExists(topicName, subscriptionName)) {
      LOG.info(
        "Subscription '{}' already exists. Deleting existing subscription.",
        subscriptionName
      );
      serviceBusAdmin.deleteSubscription(topicName, subscriptionName);
      LOG.info("Service Bus deleted subscription {}.", subscriptionName);
    }
    serviceBusAdmin.createSubscription(topicName, subscriptionName, options);

    LOG.info("{} updater created subscription {}", updaterType, subscriptionName);
  }

  /**
   * Starts the Service Bus event processor.
   */
  private void startEventProcessor() throws ServiceBusException {
    ServiceBusClientBuilder clientBuilder = new ServiceBusClientBuilder();

    if (authenticationType == AuthenticationType.FederatedIdentity) {
      Preconditions.checkNotNull(
        fullyQualifiedNamespace,
        "fullyQualifiedNamespace must be set for FederatedIdentity authentication"
      );
      clientBuilder
        .fullyQualifiedNamespace(fullyQualifiedNamespace)
        .credential(new DefaultAzureCredentialBuilder().build());
    } else if (authenticationType == AuthenticationType.SharedAccessKey) {
      Preconditions.checkNotNull(
        serviceBusUrl,
        "serviceBusUrl must be set for SharedAccessKey authentication"
      );
      clientBuilder.connectionString(serviceBusUrl);
    } else {
      throw new IllegalArgumentException("Unsupported authentication type: " + authenticationType);
    }

    eventProcessor = clientBuilder
      .processor()
      .topicName(topicName)
      .subscriptionName(subscriptionName)
      .receiveMode(ServiceBusReceiveMode.RECEIVE_AND_DELETE)
      // Receive and delete does not need autocomplete
      .disableAutoComplete()
      .prefetchCount(prefetchCount)
      .processError(this::errorConsumer)
      .processMessage(this::handleMessage)
      .buildProcessorClient();

    eventProcessor.start();
    LOG.info(
      "Service Bus processor started for topic '{}' and subscription '{}', prefetching {} messages.",
      topicName,
      subscriptionName,
      prefetchCount
    );
  }

  private void handleMessage(ServiceBusReceivedMessageContext messageContext) {
    var message = messageContext.getMessage();
    MESSAGE_COUNTER.incrementAndGet();

    if (MESSAGE_COUNTER.get() % MESSAGE_COUNTER_LOG_INTERVAL == 0) {
      LOG.debug("Total SIRI-{} messages received={}", updaterType, MESSAGE_COUNTER.get());
    }

    try {
      var siriXmlMessage = message.getBody();
      var siri = SiriXml.parseXml(siriXmlMessage.toStream());
      var serviceDelivery = siri.getServiceDelivery();
      if (serviceDelivery == null) {
        if (siri.getHeartbeatNotification() != null) {
          LOG.debug("Updater {} received SIRI heartbeat message", updaterType);
        } else {
          LOG.debug("Updater {} received SIRI message without ServiceDelivery", updaterType);
        }
      } else {
        messageHandler.handleMessage(serviceDelivery, message.getMessageId());
      }
    } catch (JAXBException | XMLStreamException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

  @Override
  public boolean isPrimed() {
    return this.isPrimed;
  }

  private void setPrimed() {
    isPrimed = true;
  }

  @Override
  public String getConfigRef() {
    return this.configRef;
  }

  /**
   * Returns None for empty result
   */
  private Optional<ServiceDelivery> fetchInitialSiriData() {
    if (dataInitializationUrl == null) {
      return Optional.empty();
    }
    var headers = HttpHeaders.of().acceptApplicationXML().build();

    LOG.info(
      "Fetching initial Siri data from {}, timeout is {} ms.",
      this.dataInitializationUrl,
      timeout
    );

    try (OtpHttpClientFactory otpHttpClientFactory = new OtpHttpClientFactory()) {
      var otpHttpClient = otpHttpClientFactory.create(LOG);
      var t1 = System.currentTimeMillis();
      var siriOptional = otpHttpClient.executeAndMapOptional(
        new HttpGet(dataInitializationUrl),
        Duration.ofMillis(timeout),
        headers,
        response -> SiriXml.parseXml(response.body())
      );
      var t2 = System.currentTimeMillis();
      LOG.info("Fetched initial data in {} ms", (t2 - t1));

      if (siriOptional.isEmpty()) {
        LOG.info("Got status 204 'No Content'.");
      }

      return siriOptional.map(Siri::getServiceDelivery);
    }
  }

  public void processInitialSiriData(ServiceDelivery serviceDelivery) {
    try {
      long t1 = System.currentTimeMillis();
      var f = messageHandler.handleMessage(serviceDelivery, "history-message");
      if (f != null) {
        f.get();
      }
      LOG.info("{} updater initialized in {} ms.", updaterType, (System.currentTimeMillis() - t1));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SiriAzureInitializationException("Interrupted while applying history", e);
    } catch (ExecutionException e) {
      throw new SiriAzureInitializationException("Error applying history", e);
    }
  }

  /**
   * Make some sensible logging on error and if Service Bus is busy, sleep for some time before try
   * again to get messages. This code snippet is taken from Microsoft example <a
   * href="https://docs.microsoft.com/sv-se/azure/service-bus-messaging/service-bus-java-how-to-use-queues">...</a>.
   *
   * @param errorContext Context for errors handled by the ServiceBusProcessorClient.
   */
  private void errorConsumer(ServiceBusErrorContext errorContext) {
    LOG.error(
      "Error when receiving messages from namespace={}, Entity={}",
      errorContext.getFullyQualifiedNamespace(),
      errorContext.getEntityPath()
    );

    if (!(errorContext.getException() instanceof ServiceBusException e)) {
      LOG.error("Non-ServiceBusException occurred!", errorContext.getException());
      return;
    }

    var reason = e.getReason();

    if (
      reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED ||
      // should this be recoverable?
      reason ==
      ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND
    ) {
      LOG.error(
        "An unrecoverable error occurred. Stopping processing with reason {} {}",
        reason,
        e.getMessage()
      );
    } else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
      LOG.error("Message lock lost for message", e);
    } else if (
      reason == ServiceBusFailureReason.SERVICE_BUSY ||
      reason == ServiceBusFailureReason.UNAUTHORIZED
    ) {
      LOG.error("Service Bus is busy or unauthorized, wait and try again");
      try {
        // Wait before retrying when Service Bus is busy or unauthorized
        TimeUnit.SECONDS.sleep(ERROR_RETRY_WAIT_SECONDS);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        LOG.info("OTP is shutting down, stopping processing of ServiceBus error messages");
      }
    } else {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

  protected OtpRetry createOtpRetry(String stepDescription) {
    return new OtpRetryBuilder()
      .withName(stepDescription)
      .withMaxAttempts(MAX_ATTEMPTS)
      .withInitialRetryInterval(INITIAL_RETRY_INTERVALS)
      .withRetryableException(e -> !(e instanceof InterruptedException))
      .withBackoffMultiplier(BACKOFF_MULTIPLIER)
      .build();
  }
}
