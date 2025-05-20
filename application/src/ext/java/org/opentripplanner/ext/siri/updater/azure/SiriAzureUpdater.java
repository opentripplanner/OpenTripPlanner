package org.opentripplanner.ext.siri.updater.azure;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusFailureReason;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationAsyncClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.CreateSubscriptionOptions;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.google.common.base.Preconditions;
import jakarta.xml.bind.JAXBException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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

  /**
   *  custom functional interface that allows throwing checked exceptions, thereby
   *  preserving the exception's intent and type.
   */
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

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private final String updaterType;
  private final AuthenticationType authenticationType;
  private final String fullyQualifiedNamespace;
  private final String configRef;
  private final String serviceBusUrl;
  private final String topicName;
  private final Duration autoDeleteOnIdle;
  private final int prefetchCount;

  private ServiceBusProcessorClient eventProcessor;
  private ServiceBusAdministrationAsyncClient serviceBusAdmin;
  private boolean isPrimed = false;
  private String subscriptionName;

  private static final AtomicLong MESSAGE_COUNTER = new AtomicLong(0);

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
    // In Kubernetes this should be the POD identifier
    subscriptionName = System.getenv("HOSTNAME");
    if (subscriptionName == null || subscriptionName.isBlank()) {
      subscriptionName = "otp-" + UUID.randomUUID();
    }

    try {
      executeWithRetry(this::setupSubscription, "Setting up Service Bus subscription to topic");

      executeWithRetry(
        () -> {
          var initialData = fetchInitialSiriData();
          if (initialData.isEmpty()) {
            LOG.info("Got empty response from history endpoint");
          } else {
            processInitialSiriData(initialData.get());
          }
        },
        "Initializing historical Siri data"
      );

      executeWithRetry(this::startEventProcessor, "Starting Service Bus event processor");

      setPrimed();

      ApplicationShutdownSupport.addShutdownHook("azure-siri-updater-shutdown", () -> {
        LOG.info("Calling shutdownHook on AbstractAzureSiriUpdater");
        if (eventProcessor != null) {
          eventProcessor.close();
        }
        if (serviceBusAdmin != null) {
          serviceBusAdmin.deleteSubscription(topicName, subscriptionName).block();
          LOG.info("Subscription '{}' deleted on topic '{}'.", subscriptionName, topicName);
        }
      });
    } catch (ServiceBusException e) {
      LOG.error("Service Bus encountered an error during setup: {}", e.getMessage(), e);
    } catch (URISyntaxException e) {
      LOG.error("Invalid URI provided for Service Bus setup: {}", e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.warn("Updater was interrupted during setup.");
    } catch (Exception e) {
      LOG.error("An unexpected error occurred during setup: {}", e.getMessage(), e);
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
   * Executes a task with retry logic. Retries indefinitely for retryable exceptions with exponential backoff.
   *  Does not retry for InterruptedException and propagates it
   * @param task The task to execute.
   * @param description A description of the task for logging purposes.
   * @throws InterruptedException If the thread is interrupted while waiting between retries.
   */
  void executeWithRetry(CheckedRunnable task, String description) throws Exception {
    int sleepPeriod = 1000; // Start with 1-second delay
    int attemptCounter = 1;

    while (true) {
      try {
        task.run();
        LOG.info("{} succeeded.", description);
        return;
      } catch (InterruptedException ie) {
        LOG.warn("{} was interrupted during execution.", description);
        Thread.currentThread().interrupt(); // Restore interrupted status
        throw ie;
      } catch (Exception e) {
        LOG.warn("{} failed. Error: {} (Attempt {})", description, e.getMessage(), attemptCounter);

        if (!shouldRetry(e)) {
          LOG.error("{} encountered a non-retryable error: {}.", description, e.getMessage());
          throw e; // Stop retries if the error is non-retryable
        }

        LOG.warn("{} will retry in {} ms.", description, sleepPeriod);
        attemptCounter++;
        try {
          sleep(sleepPeriod);
        } catch (InterruptedException ie) {
          LOG.warn("{} was interrupted during sleep.", description);
          Thread.currentThread().interrupt(); // Restore interrupted status
          throw ie;
        }
        sleepPeriod = Math.min(sleepPeriod * 2, 60 * 1000); // Exponential backoff with a cap at 60 seconds
      }
    }
  }

  boolean shouldRetry(Exception e) {
    if (e instanceof ServiceBusException sbException) {
      ServiceBusFailureReason reason = sbException.getReason();

      if (RETRYABLE_REASONS.contains(reason)) {
        LOG.warn("Transient error encountered: {}. Retrying...", reason);
        return true;
      } else if (NON_RETRYABLE_REASONS.contains(reason)) {
        LOG.error("Non-recoverable error encountered: {}. Not retrying.", reason);
        return false;
      } else {
        LOG.warn("Unhandled ServiceBusFailureReason: {}. Retrying by default.", reason);
        return true;
      }
    } else if (ExceptionUtils.hasCause(e, OtpHttpClientException.class)) {
      // retry for OtpHttpClientException as it is thrown if historical data can't be read at the moment
      return true;
    }

    LOG.warn("Non-ServiceBus exception encountered: {}. Not retrying.", e.getClass().getName());
    return false;
  }

  /**
   * Sets up the Service Bus subscription, including checking old subscription, deleting if necessary,
   * and creating a new subscription.
   */
  private void setupSubscription() throws ServiceBusException, URISyntaxException {
    // Client with permissions to create subscription
    if (authenticationType == AuthenticationType.FederatedIdentity) {
      serviceBusAdmin = new ServiceBusAdministrationClientBuilder()
        .credential(fullyQualifiedNamespace, new DefaultAzureCredentialBuilder().build())
        .buildAsyncClient();
    } else if (authenticationType == AuthenticationType.SharedAccessKey) {
      serviceBusAdmin = new ServiceBusAdministrationClientBuilder()
        .connectionString(serviceBusUrl)
        .buildAsyncClient();
    }

    // Set options
    CreateSubscriptionOptions options = new CreateSubscriptionOptions()
      .setDefaultMessageTimeToLive(Duration.of(25, ChronoUnit.HOURS))
      .setAutoDeleteOnIdle(autoDeleteOnIdle);

    // Make sure there is no old subscription on serviceBus
    if (
      Boolean.TRUE.equals(
        serviceBusAdmin.getSubscriptionExists(topicName, subscriptionName).block()
      )
    ) {
      LOG.info(
        "Subscription '{}' already exists. Deleting existing subscription.",
        subscriptionName
      );
      serviceBusAdmin.deleteSubscription(topicName, subscriptionName).block();
      LOG.info("Service Bus deleted subscription {}.", subscriptionName);
    }
    serviceBusAdmin.createSubscription(topicName, subscriptionName, options).block();

    LOG.info("{} created subscription {}", getClass().getSimpleName(), subscriptionName);
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

    if (MESSAGE_COUNTER.get() % 100 == 0) {
      LOG.debug("Total SIRI-{} messages received={}", updaterType, MESSAGE_COUNTER.get());
    }

    try {
      var siriXmlMessage = message.getBody().toString();
      var siri = SiriXml.parseXml(siriXmlMessage);
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
    var headers = HttpHeaders.of().acceptApplicationXML().build().asMap();

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
        SiriXml::parseXml
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
      reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND // should this  be recoverable?
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
        // Choosing an arbitrary amount of time to wait until trying again.
        TimeUnit.SECONDS.sleep(5);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        LOG.info("OTP is shutting down, stopping processing of ServiceBus error messages");
      }
    } else {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }
}
