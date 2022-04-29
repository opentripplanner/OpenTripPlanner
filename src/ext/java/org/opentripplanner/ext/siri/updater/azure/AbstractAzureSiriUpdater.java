package org.opentripplanner.ext.siri.updater.azure;

import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationAsyncClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.CreateSubscriptionOptions;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactory;

public abstract class AbstractAzureSiriUpdater implements GraphUpdater {

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private boolean isPrimed = false;
  protected WriteToGraphCallback saveResultOnGraph;
  protected SiriTimetableSnapshotSource snapshotSource;
  private final String configRef;

  private ServiceBusProcessorClient eventProcessor;
  private final Consumer<ServiceBusReceivedMessageContext> messageConsumer = this::messageConsumer;
  private final Consumer<ServiceBusErrorContext> errorConsumer = this::errorConsumer;
  private ServiceBusAdministrationAsyncClient serviceBusAdmin;
  protected final String topicName;
  private String subscriptionName;
  private final String serviceBusUrl;
  protected String feedId;

  /**
   * The URL used to fetch all initial updates
   */
  private final String dataInitializationUrl;
  /**
   * The timeout used when fetching historical data
   */
  protected int timeout;

  public AbstractAzureSiriUpdater(SiriAzureUpdaterParameters config) {
    this.configRef = config.getConfigRef();

    this.serviceBusUrl = config.getServiceBusUrl();
    this.topicName = config.getTopicName();

    this.dataInitializationUrl = config.getDataInitializationUrl();
    this.timeout = config.getTimeout();
    this.feedId = config.getFeedId();
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
  public void setup(Graph graph) throws Exception {
    snapshotSource = graph.getOrSetupTimetableSnapshotProvider(SiriTimetableSnapshotSource::new);
  }

  @Override
  public void run() throws Exception {
    Preconditions.checkNotNull(topicName, "'topic' must be set");
    Preconditions.checkNotNull(serviceBusUrl, "'servicebus-url' must be set");
    Preconditions.checkNotNull(feedId, "'feedId' must be set");
    Preconditions.checkState(feedId.length() > 0, "'feedId' must be set");

    // In Kubernetes this should be the POD identifier
    subscriptionName = System.getenv("HOSTNAME");
    if (subscriptionName == null || subscriptionName.isBlank()) {
      subscriptionName = "otp-" + UUID.randomUUID();
    }

    // Client with permissions to create subscription
    serviceBusAdmin =
      new ServiceBusAdministrationClientBuilder()
        .connectionString(serviceBusUrl)
        .buildAsyncClient();

    // If Idle more then one day, then delete subscription so we don't have old obsolete subscriptions on Azure Service Bus
    var options = new CreateSubscriptionOptions();
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

    try {
      Runtime.getRuntime().addShutdownHook(new Thread(this::teardown));
    } catch (IllegalStateException e) {
      LOG.error(e.getLocalizedMessage(), e);
      teardown();
    }
  }

  @Override
  public void teardown() {
    eventProcessor.stop();
    serviceBusAdmin.deleteSubscription(topicName, subscriptionName).block();
    LOG.info("Subscription {} deleted on topic {}", subscriptionName, topicName);
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

  /**
   * InitializeData - wrapping method that calls an implementation of initialize data - and blocks readiness till finished
   */
  private void initializeData() {
    int sleepPeriod = 1000;
    int attemptCounter = 1;
    while (true) {
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
        } catch (InterruptedException interruptedException) {
          //Ignore
        }
      }
    }
  }

  protected abstract void initializeData(
    String url,
    Consumer<ServiceBusReceivedMessageContext> consumer
  ) throws IOException, URISyntaxException;

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
      } catch (InterruptedException e2) {
        LOG.error("Unable to sleep for period of time");
      }
    } else {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

  /**
   * @return the current datetime adjusted to the current timezone
   */
  protected long now() {
    return ZonedDateTime.now().toInstant().toEpochMilli();
  }
}
