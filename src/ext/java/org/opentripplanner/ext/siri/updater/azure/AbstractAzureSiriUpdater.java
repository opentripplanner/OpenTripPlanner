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
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.opentripplanner.ext.siri.EntityResolver;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.framework.application.ApplicationShutdownSupport;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAzureSiriUpdater implements GraphUpdater {

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private final AuthenticationType authenticationType;
  private final String fullyQualifiedNamespace;
  private final String configRef;
  private final String serviceBusUrl;
  private final SiriFuzzyTripMatcher fuzzyTripMatcher;
  private final EntityResolver entityResolver;
  private final Consumer<ServiceBusReceivedMessageContext> messageConsumer = this::messageConsumer;
  private final Consumer<ServiceBusErrorContext> errorConsumer = this::errorConsumer;
  private final String topicName;

  protected WriteToGraphCallback saveResultOnGraph;
  private ServiceBusProcessorClient eventProcessor;
  private ServiceBusAdministrationAsyncClient serviceBusAdmin;
  private boolean isPrimed = false;
  private String subscriptionName;

  protected final String feedId;

  /**
   * The URL used to fetch all initial updates
   */
  private final String dataInitializationUrl;
  /**
   * The timeout used when fetching historical data
   */
  protected final int timeout;

  public AbstractAzureSiriUpdater(SiriAzureUpdaterParameters config, TransitModel transitModel) {
    this.configRef = config.configRef();
    this.authenticationType = config.getAuthenticationType();
    this.fullyQualifiedNamespace = config.getFullyQualifiedNamespace();
    this.serviceBusUrl = config.getServiceBusUrl();
    this.topicName = config.getTopicName();
    this.dataInitializationUrl = config.getDataInitializationUrl();
    this.timeout = config.getTimeout();
    this.feedId = config.feedId();
    TransitService transitService = new DefaultTransitService(transitModel);
    this.entityResolver = new EntityResolver(transitService, feedId);
    this.fuzzyTripMatcher =
      config.isFuzzyTripMatching() ? SiriFuzzyTripMatcher.of(transitService) : null;
  }

  /**
   * Consume Service Bus topic message and implement business logic.
   * @param messageContext The Service Bus processor message context that holds a received message and additional methods to settle the message.
   */
  protected abstract void messageConsumer(ServiceBusReceivedMessageContext messageContext);

  /**
   * Consume error and decide how to manage it.
   * @param errorContext Context for errors handled by the ServiceBusProcessorClient.
   */
  protected abstract void errorConsumer(ServiceBusErrorContext errorContext);

  @Override
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
  }

  @Override
  public void run() {
    Objects.requireNonNull(topicName, "'topic' must be set");
    Objects.requireNonNull(serviceBusUrl, "'servicebus-url' must be set");
    Objects.requireNonNull(feedId, "'feedId' must be set");
    Preconditions.checkState(feedId.length() > 0, "'feedId' must be set");

    // In Kubernetes this should be the POD identifier
    subscriptionName = System.getenv("HOSTNAME");
    if (subscriptionName == null || subscriptionName.isBlank()) {
      subscriptionName = "otp-" + UUID.randomUUID();
    }

    // Client with permissions to create subscription
    if (authenticationType == AuthenticationType.FederatedIdentity) {
      serviceBusAdmin =
        new ServiceBusAdministrationClientBuilder()
          .credential(fullyQualifiedNamespace, new DefaultAzureCredentialBuilder().build())
          .buildAsyncClient();
    } else if (authenticationType == AuthenticationType.SharedAccessKey) {
      serviceBusAdmin =
        new ServiceBusAdministrationClientBuilder()
          .connectionString(serviceBusUrl)
          .buildAsyncClient();
    }

    // If Idle more then one day, then delete subscription so we don't have old obsolete subscriptions on Azure Service Bus
    var options = new CreateSubscriptionOptions();
    options.setDefaultMessageTimeToLive(Duration.of(25, ChronoUnit.HOURS));
    options.setAutoDeleteOnIdle(Duration.ofDays(1));

    // Make sure there is no old subscription on serviceBus
    if (
      Boolean.TRUE.equals(
        serviceBusAdmin.getSubscriptionExists(topicName, subscriptionName).block()
      )
    ) {
      LOG.info("Subscription {} already exists", subscriptionName);
      serviceBusAdmin.deleteSubscription(topicName, subscriptionName).block();
      LOG.info("Service Bus deleted subscription {}.", subscriptionName);
    }
    serviceBusAdmin.createSubscription(topicName, subscriptionName, options).block();

    LOG.info("Service Bus created subscription {}", subscriptionName);

    // Initialize historical Siri data
    initializeData();

    eventProcessor =
      new ServiceBusClientBuilder()
        .connectionString(serviceBusUrl)
        .processor()
        .topicName(topicName)
        .subscriptionName(subscriptionName)
        .processError(errorConsumer)
        .processMessage(messageConsumer)
        .buildProcessorClient();

    eventProcessor.start();
    LOG.info(
      "Service Bus processor started for topic {} and subscription {}",
      topicName,
      subscriptionName
    );

    ApplicationShutdownSupport.addShutdownHook(
      "azure-siri-updater-shutdown",
      () -> {
        LOG.info("Calling shutdownHook on AbstractAzureSiriUpdater");
        eventProcessor.close();
        serviceBusAdmin.deleteSubscription(topicName, subscriptionName).block();
        LOG.info("Subscription '{}' deleted on topic '{}'.", subscriptionName, topicName);
      }
    );
  }

  @Override
  public boolean isPrimed() {
    return this.isPrimed;
  }

  public void setPrimed(boolean primed) {
    isPrimed = primed;
  }

  @Override
  public String getConfigRef() {
    return this.configRef;
  }

  protected String fetchInitialData(URI uri) {
    // Maybe put this in the config?
    HttpHeaders rh = HttpHeaders.of().acceptApplicationXML().build();
    String initialData;
    try (OtpHttpClient otpHttpClient = new OtpHttpClient()) {
      initialData =
        otpHttpClient.getAndMap(
          uri,
          Duration.ofMillis(timeout),
          rh.asMap(),
          is -> CharStreams.toString(new InputStreamReader(is))
        );
    }
    return initialData;
  }

  SiriFuzzyTripMatcher fuzzyTripMatcher() {
    return fuzzyTripMatcher;
  }

  EntityResolver entityResolver() {
    return entityResolver;
  }

  /**
   * InitializeData - wrapping method that calls an implementation of initialize data - and blocks readiness till finished
   */
  private void initializeData() {
    int sleepPeriod = 1000;
    int attemptCounter = 1;
    boolean otpIsShuttingDown = false;

    while (!otpIsShuttingDown) {
      try {
        initializeData(dataInitializationUrl, messageConsumer);
        break;
      } catch (Exception e) {
        sleepPeriod = sleepPeriod * 2;

        LOG.warn(
          "Caught exception while initializing data will retry after {} ms - attempt {}. ({})",
          sleepPeriod,
          attemptCounter++,
          e.toString()
        );
        try {
          Thread.sleep(sleepPeriod);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          otpIsShuttingDown = true;
          LOG.info("OTP is shutting down, cancelling attempt to initialize Azure SIRI Updater.");
        }
      }
    }
  }

  protected abstract void initializeData(
    String url,
    Consumer<ServiceBusReceivedMessageContext> consumer
  ) throws URISyntaxException;

  /**
   * Make some sensible logging on error and if Service Bus is busy, sleep for some time before try again to get messages.
   * This code snippet is taken from Microsoft example https://docs.microsoft.com/sv-se/azure/service-bus-messaging/service-bus-java-how-to-use-queues.
   * @param errorContext Context for errors handled by the ServiceBusProcessorClient.
   */
  protected void defaultErrorConsumer(ServiceBusErrorContext errorContext) {
    LOG.error(
      "Error when receiving messages from namespace={}, Entity={}",
      errorContext.getFullyQualifiedNamespace(),
      errorContext.getEntityPath()
    );

    if (!(errorContext.getException() instanceof ServiceBusException)) {
      LOG.error("Non-ServiceBusException occurred!", errorContext.getException());
      return;
    }

    var e = (ServiceBusException) errorContext.getException();
    var reason = e.getReason();

    if (
      reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED ||
      reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND ||
      reason == ServiceBusFailureReason.UNAUTHORIZED
    ) {
      LOG.error(
        "An unrecoverable error occurred. Stopping processing with reason {} {}",
        reason,
        e.getMessage()
      );
    } else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
      LOG.error("Message lock lost for message", e);
    } else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {
      LOG.error("Service Bus is busy, wait and try again");
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
