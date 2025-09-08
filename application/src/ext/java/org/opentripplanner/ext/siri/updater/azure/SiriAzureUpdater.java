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
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.entur.siri21.util.SiriXml;
import org.opentripplanner.framework.application.ApplicationShutdownSupport;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.alert.TransitAlertProvider;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.siri.SiriRealTimeTripUpdateAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.ServiceDelivery;
import uk.org.siri.siri21.Siri;

/**
 * This is the main handler for siri messages over azure. It handles the generic code for communicating
 * with the azure service bus and delegates to SiriAzureETUpdater and SiriAzureSXUpdater for ET and
 * SX specific stuff.
 */
public class SiriAzureUpdater implements GraphUpdater {

  @FunctionalInterface
  interface CheckedRunnable {
    void run() throws Exception;
  }

  private static final Set<ServiceBusFailureReason> RETRYABLE_REASONS = Set.of(
    ServiceBusFailureReason.GENERAL_ERROR,
    ServiceBusFailureReason.QUOTA_EXCEEDED,
    ServiceBusFailureReason.SERVICE_BUSY,
    ServiceBusFailureReason.SERVICE_COMMUNICATION_ERROR,
    ServiceBusFailureReason.SERVICE_TIMEOUT,
    ServiceBusFailureReason.UNAUTHORIZED,
    ServiceBusFailureReason.MESSAGE_LOCK_LOST,
    ServiceBusFailureReason.SESSION_LOCK_LOST,
    ServiceBusFailureReason.SESSION_CANNOT_BE_LOCKED
  );

  private static final Set<ServiceBusFailureReason> NON_RETRYABLE_REASONS = Set.of(
    ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND,
    ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED,
    ServiceBusFailureReason.MESSAGE_SIZE_EXCEEDED,
    ServiceBusFailureReason.MESSAGE_NOT_FOUND,
    ServiceBusFailureReason.MESSAGING_ENTITY_ALREADY_EXISTS
  );

  private static final Logger log = LoggerFactory.getLogger(SiriAzureUpdater.class);
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
  private static final int MESSAGE_COUNTER_LOG_INTERVAL = 100;
  private static final int ERROR_RETRY_WAIT_SECONDS = 5;
  private static final int INITIAL_RETRY_DELAY_MS = 1000;
  private static final int MAX_RETRY_DELAY_MS = 60_000;

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

  private final Duration startupTimeout;

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
    this.startupTimeout = config.getStartupTimeout();
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
   * This wrapper class is a SiriAzureUpdater that implements the TransitAlertProvider interface so it can
   * be registered to handle SX messages. It delegates the actual SIRI-SX
   * message processing to the contained SiriAzureSXUpdater.
   */
  public static class SxWrapper extends SiriAzureUpdater implements TransitAlertProvider {

    SxWrapper(SiriAzureUpdaterParameters config, SiriAzureSXUpdater messageHandler) {
      super(config, messageHandler);
    }

    /**
     * Implements the TransitAlertProvider interface to allow this updater to be detected
     * as a source of transit alerts. This method delegates to the internal SiriAzureSXUpdater
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
      tryStartupStep(this::setupSubscription, "ServiceBusSubscription");

      tryStartupStep(
        () -> {
          var initialData = fetchInitialSiriData();
          if (initialData.isEmpty()) {
            log.info("Got empty response from history endpoint");
          } else {
            processInitialSiriData(initialData.get());
          }
        },
        "HistoricalSiriData"
      );

      tryStartupStep(this::startEventProcessor, "ServiceBusEventProcessor");

      // Set primed so OTP can start
      setPrimed();

      // Register shutdown hook only once, and only after subscriptionName is set
      registerShutdownHook();
    } catch (InterruptedException e) {
      log.info("Startup interrupted, aborting updater initialization");
      Thread.currentThread().interrupt(); // Preserve interrupt status
      // Don't set primed, don't register shutdown hook - just exit
    }
  }

  /**
   * Sleeps. This is to be able to mock testing
   * @param millis number of milliseconds
   * @throws InterruptedException if sleep is interrupted
   */
  void sleep(int millis) throws InterruptedException {
    Thread.sleep(millis);
  }

  /**
   * Executes a task with retry logic with timeout constraint.
   * Retries for retryable exceptions with exponential backoff.
   *
   * @param task The task to execute
   * @param description A description for logging
   * @param timeoutMs Timeout in milliseconds
   * @return true if task completed successfully, false if timeout was exceeded
   * @throws InterruptedException If interrupted
   * @throws Exception Any non-retryable exception from the task
   */
  boolean executeWithRetry(CheckedRunnable task, String description, long timeoutMs)
    throws Exception {
    int sleepPeriod = INITIAL_RETRY_DELAY_MS;
    int attemptCounter = 1;
    long startTime = System.currentTimeMillis();

    while (System.currentTimeMillis() - startTime < timeoutMs) {
      try {
        task.run();
        log.info("{} succeeded after {} attempts.", description, attemptCounter);
        return true;
      } catch (InterruptedException ie) {
        log.warn("{} was interrupted.", description);
        Thread.currentThread().interrupt();
        throw ie;
      } catch (Exception e) {
        log.warn("{} failed. Error: {} (Attempt {})", description, e.getMessage(), attemptCounter);

        if (!shouldRetry(e)) {
          log.error("{} encountered a non-retryable error: {}.", description, e.getMessage());
          throw e;
        }

        log.debug("{} will retry in {} ms.", description, sleepPeriod);
        attemptCounter++;

        try {
          sleep(sleepPeriod);
        } catch (InterruptedException ie) {
          log.warn("{} was interrupted during sleep.", description);
          Thread.currentThread().interrupt();
          throw ie;
        }

        sleepPeriod = Math.min(sleepPeriod * 2, MAX_RETRY_DELAY_MS);
      }
    }

    // Timeout exceeded
    log.warn("{} timed out after {} ms", description, timeoutMs);
    return false;
  }

  boolean shouldRetry(Exception e) {
    if (e instanceof ServiceBusException sbException) {
      ServiceBusFailureReason reason = sbException.getReason();

      if (RETRYABLE_REASONS.contains(reason)) {
        log.warn("Transient error encountered: {}. Retrying...", reason);
        return true;
      } else if (NON_RETRYABLE_REASONS.contains(reason)) {
        log.error("Non-recoverable error encountered: {}. Not retrying.", reason);
        return false;
      } else {
        log.warn("Unhandled ServiceBusFailureReason: {}. Retrying by default.", reason);
        return true;
      }
    } else if (ExceptionUtils.hasCause(e, OtpHttpClientException.class)) {
      // retry for OtpHttpClientException as it is thrown if historical data can't be read at the moment
      return true;
    } else if (getNetworkErrorType(e).isPresent()) {
      log.warn(
        "Network connectivity error encountered: {}. Retrying...",
        getNetworkErrorType(e).get()
      );
      return true;
    }

    log.warn("Non-ServiceBus exception encountered: {}. Not retrying.", e.getClass().getName());
    return false;
  }

  /**
   * Checks if the exception represents a transient network connectivity issue.
   * @return Optional with error description if it's a network error, empty otherwise
   */
  private Optional<String> getNetworkErrorType(Exception e) {
    // DNS resolution failures - commonly transient (DNS server issues, VPN connectivity)
    if (ExceptionUtils.hasCause(e, UnknownHostException.class)) {
      return Optional.of("DNS resolution failure");
    }

    if (ExceptionUtils.hasCause(e, SocketTimeoutException.class)) {
      return Optional.of("Socket timeout");
    }

    // Check for ConnectTimeoutException wrapped in UncheckedIOException
    if (ExceptionUtils.hasCause(e, UncheckedIOException.class)) {
      Throwable cause = ExceptionUtils.getRootCause(e);
      if (cause != null && cause.getClass().getSimpleName().contains("ConnectTimeoutException")) {
        return Optional.of("Connection timeout");
      }
    }

    return Optional.empty();
  }

  /**
   * Attempts to execute a startup step with timeout.
   * Logs errors but continues execution for graceful degradation.
   * Rethrows InterruptedException to abort startup process.
   */
  private void tryStartupStep(CheckedRunnable task, String stepDescription)
    throws InterruptedException {
    try {
      boolean success = executeWithRetry(task, stepDescription, startupTimeout.toMillis());
      if (success) {
        log.info("{} completed successfully", stepDescription);
      } else {
        log.warn(
          "REALTIME_STARTUP_ALERT component={} status=TIMEOUT error=\"{} timed out after {} ms\"",
          stepDescription,
          stepDescription,
          startupTimeout.toMillis()
        );
      }
    } catch (InterruptedException e) {
      // Rethrow to abort startup process and avoid blocking JVM shutdown
      log.warn(
        "REALTIME_STARTUP_ALERT component={} status=INTERRUPTED error=\"Aborting startup due to interrupt\"",
        stepDescription
      );
      Thread.currentThread().interrupt(); // Preserve interrupt status
      throw e;
    } catch (Exception e) {
      String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      log.warn(
        "REALTIME_STARTUP_ALERT component={} status=FAILED error=\"{}\"",
        stepDescription,
        message
      );
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
    log.info("Starting shutdown for {} updater", updaterType);

    // 1. Close event processor
    if (eventProcessor != null) {
      try {
        eventProcessor.close();
        log.debug("Event processor closed successfully");
      } catch (Exception e) {
        log.warn("Error closing event processor: {}", e.getMessage());
      }
    }

    // 2. Delete subscription if we have admin client and subscription name
    if (serviceBusAdmin != null && subscriptionName != null) {
      try {
        serviceBusAdmin.deleteSubscription(topicName, subscriptionName);
        log.info("Subscription '{}' deleted on topic '{}'", subscriptionName, topicName);
      } catch (Exception e) {
        log.warn("Error deleting subscription '{}': {}", subscriptionName, e.getMessage());
      }
    }

    log.info("Shutdown complete for {} updater", updaterType);
  }

  /**
   * Sets up the Service Bus subscription, including checking old subscription, deleting if necessary,
   * and creating a new subscription.
   */
  private void setupSubscription() throws ServiceBusException, URISyntaxException {
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
    CreateSubscriptionOptions options = new CreateSubscriptionOptions()
      .setAutoDeleteOnIdle(autoDeleteOnIdle);

    // Make sure there is no old subscription on serviceBus
    if (serviceBusAdmin.getSubscriptionExists(topicName, subscriptionName)) {
      log.info(
        "Subscription '{}' already exists. Deleting existing subscription.",
        subscriptionName
      );
      serviceBusAdmin.deleteSubscription(topicName, subscriptionName);
      log.info("Service Bus deleted subscription {}.", subscriptionName);
    }
    serviceBusAdmin.createSubscription(topicName, subscriptionName, options);

    log.info("{} updater created subscription {}", updaterType, subscriptionName);
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
      .disableAutoComplete() // Receive and delete does not need autocomplete
      .prefetchCount(prefetchCount)
      .processError(this::errorConsumer)
      .processMessage(this::handleMessage)
      .buildProcessorClient();

    eventProcessor.start();
    log.info(
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
      log.debug("Total SIRI-{} messages received={}", updaterType, MESSAGE_COUNTER.get());
    }

    try {
      var siriXmlMessage = message.getBody().toString();
      var siri = SiriXml.parseXml(siriXmlMessage);
      var serviceDelivery = siri.getServiceDelivery();
      if (serviceDelivery == null) {
        if (siri.getHeartbeatNotification() != null) {
          log.debug("Updater {} received SIRI heartbeat message", updaterType);
        } else {
          log.debug("Updater {} received SIRI message without ServiceDelivery", updaterType);
        }
      } else {
        messageHandler.handleMessage(serviceDelivery, message.getMessageId());
      }
    } catch (JAXBException | XMLStreamException e) {
      log.error(e.getLocalizedMessage(), e);
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
    var headers = HttpHeaders.of().acceptApplicationXML().build().asMap();

    log.info(
      "Fetching initial Siri data from {}, timeout is {} ms.",
      this.dataInitializationUrl,
      timeout
    );

    try (OtpHttpClientFactory otpHttpClientFactory = new OtpHttpClientFactory()) {
      var otpHttpClient = otpHttpClientFactory.create(log);
      var t1 = System.currentTimeMillis();
      var siriOptional = otpHttpClient.executeAndMapOptional(
        new HttpGet(dataInitializationUrl),
        Duration.ofMillis(timeout),
        headers,
        SiriXml::parseXml
      );
      var t2 = System.currentTimeMillis();
      log.info("Fetched initial data in {} ms", (t2 - t1));

      if (siriOptional.isEmpty()) {
        log.info("Got status 204 'No Content'.");
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
      log.info("{} updater initialized in {} ms.", updaterType, (System.currentTimeMillis() - t1));
    } catch (ExecutionException | InterruptedException e) {
      throw new SiriAzureInitializationException("Error applying history", e);
    }
  }

  /**
   * Make some sensible logging on error and if Service Bus is busy, sleep for some time before try again to get messages.
   * This code snippet is taken from Microsoft example <a href="https://docs.microsoft.com/sv-se/azure/service-bus-messaging/service-bus-java-how-to-use-queues">...</a>.
   * @param errorContext Context for errors handled by the ServiceBusProcessorClient.
   */
  private void errorConsumer(ServiceBusErrorContext errorContext) {
    log.error(
      "Error when receiving messages from namespace={}, Entity={}",
      errorContext.getFullyQualifiedNamespace(),
      errorContext.getEntityPath()
    );

    if (!(errorContext.getException() instanceof ServiceBusException e)) {
      log.error("Non-ServiceBusException occurred!", errorContext.getException());
      return;
    }

    var reason = e.getReason();

    if (
      reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED ||
      reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND // should this be recoverable?
    ) {
      log.error(
        "An unrecoverable error occurred. Stopping processing with reason {} {}",
        reason,
        e.getMessage()
      );
    } else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
      log.error("Message lock lost for message", e);
    } else if (
      reason == ServiceBusFailureReason.SERVICE_BUSY ||
      reason == ServiceBusFailureReason.UNAUTHORIZED
    ) {
      log.error("Service Bus is busy or unauthorized, wait and try again");
      try {
        // Wait before retrying when Service Bus is busy or unauthorized
        TimeUnit.SECONDS.sleep(ERROR_RETRY_WAIT_SECONDS);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        log.info("OTP is shutting down, stopping processing of ServiceBus error messages");
      }
    } else {
      log.error(e.getLocalizedMessage(), e);
    }
  }
}
