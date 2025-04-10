package org.opentripplanner.updater.trip.siri.updater.google;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ExpirationPolicy;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import javax.xml.stream.XMLStreamException;
import org.entur.siri21.util.SiriXml;
import org.opentripplanner.framework.application.ApplicationShutdownSupport;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.framework.retry.OtpRetry;
import org.opentripplanner.framework.retry.OtpRetryBuilder;
import org.opentripplanner.updater.trip.siri.updater.AsyncEstimatedTimetableSource;
import org.opentripplanner.utils.text.FileSizeToTextConverter;
import org.opentripplanner.utils.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.ServiceDelivery;
import uk.org.siri.siri21.Siri;

/**
 * A source of estimated timetables that reads SIRI-ET messages from a Google PubSub subscription.
 * <p>
 *   This class starts a Google PubSub subscription
 * <p>
 * NOTE: - Path to Google credentials (.json-file) MUST exist in environment-variable
 * "GOOGLE_APPLICATION_CREDENTIALS" as described here:
 * <a href="https://cloud.google.com/docs/authentication/getting-started">ServiceAccount need access
 * to
 * create subscription ("editor")</a>
 * <p>
 * <p>
 * <p>
 * Startup-flow: 1. Create subscription to topic. Subscription will receive all updates after
 * creation. 2. Fetch current data to initialize state. 3. Flag updater as initialized 3. Start
 * receiving updates from Pubsub-subscription
 *
 *
 * <pre>
 *   "type": "google-pubsub-siri-et-updater",
 *   "projectName":"project-1234",                                                      // Google Cloud project name
 *   "topicName": "xml.estimated_timetables",                                           // Google Cloud Pubsub topic
 *   "dataInitializationUrl": "http://server/realtime/xml/et"  // Optional URL used to initialize OTP with all existing data
 * </pre>
 */
public class GooglePubsubEstimatedTimetableSource implements AsyncEstimatedTimetableSource {

  private static final Logger LOG = LoggerFactory.getLogger(
    GooglePubsubEstimatedTimetableSource.class
  );

  private static final AtomicLong MESSAGE_COUNTER = new AtomicLong(0);
  private static final AtomicLong UPDATE_COUNTER = new AtomicLong(0);
  private static final AtomicLong SIZE_COUNTER = new AtomicLong(0);
  private static final String SUBSCRIPTION_PREFIX = "siri-et-";

  private static final int RETRY_MAX_ATTEMPTS = Integer.MAX_VALUE;
  private static final Duration RETRY_INITIAL_DELAY = Duration.ofSeconds(1);
  private static final int RETRY_BACKOFF = 2;

  /**
   * The URL used to fetch all initial updates.
   * The URL responds to HTTP GET and returns all initial data in xml-format. It will be
   * called once to initialize real-time-data.
   * All subsequent updates will be received from Google Cloud Pubsub.
   */
  private final URI dataInitializationUrl;

  /**
   * The time to wait before reconnecting after a failed connection.
   */
  private final Duration reconnectPeriod;

  /**
   * For larger deployments it sometimes takes more than the default 30 seconds to fetch data, if so
   * this parameter can be increased.
   */
  private final Duration initialGetDataTimeout;

  private final String subscriptionName;
  private final ProjectTopicName topic;
  private final Subscriber subscriber;
  private final PushConfig pushConfig;
  private final Instant startTime = Instant.now();

  private final OtpRetry retry;

  private Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer;
  private volatile boolean primed;

  public GooglePubsubEstimatedTimetableSource(
    String dataInitializationUrl,
    Duration reconnectPeriod,
    Duration initialGetDataTimeout,
    String subscriptionProjectName,
    String topicProjectName,
    String topicName
  ) {
    //
    this.dataInitializationUrl = URI.create(dataInitializationUrl);
    this.reconnectPeriod = reconnectPeriod;
    this.initialGetDataTimeout = initialGetDataTimeout;

    String subscriptionId = buildSubscriptionId();
    subscriptionName = ProjectSubscriptionName.of(
      subscriptionProjectName,
      subscriptionId
    ).toString();
    subscriber = Subscriber.newBuilder(
      subscriptionName,
      new EstimatedTimetableMessageReceiver()
    ).build();
    this.topic = ProjectTopicName.of(topicProjectName, topicName);
    this.pushConfig = PushConfig.getDefaultInstance();

    retry = new OtpRetryBuilder()
      .withName("SIRI-ET Google PubSub Updater setup")
      .withMaxAttempts(RETRY_MAX_ATTEMPTS)
      .withInitialRetryInterval(RETRY_INITIAL_DELAY)
      .withBackoffMultiplier(RETRY_BACKOFF)
      .build();

    addShutdownHook();
  }

  /**
   * Create a PubSub subscription, read the backlog of messages and start listening to the
   * subscription.
   * Enter an infinite loop waiting for messages. An interruption sent at server
   * shutdown will cause the loop to stop.
   */
  @Override
  public void start(Function<ServiceDelivery, Future<?>> serviceDeliveryConsumer) {
    this.serviceDeliveryConsumer = serviceDeliveryConsumer;

    try {
      LOG.info("Creating subscription {}", subscriptionName);
      retry.execute(this::createSubscription);
      LOG.info("Created subscription {}", subscriptionName);

      // Retrying until data is initialized successfully
      retry.execute(this::initializeData);

      while (true) {
        try {
          subscriber.startAsync().awaitRunning();
          primed = true;
          subscriber.awaitTerminated();
        } catch (IllegalStateException e) {
          subscriber.stopAsync();
        }
        Thread.sleep(reconnectPeriod.toMillis());
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      LOG.info("OTP is shutting down, stopping the SIRI ET Google PubSub Updater.");
    }
  }

  @Override
  public boolean isPrimed() {
    return primed;
  }

  /**
   * Build a unique name for the subscription.
   * This ensures that if the subscription is not properly deleted during shutdown,
   * a restarted instance will get a fresh subscription.
   */
  private static String buildSubscriptionId() {
    String hostname = System.getenv("HOSTNAME");
    if (hostname == null || hostname.isEmpty()) {
      return SUBSCRIPTION_PREFIX + "otp-" + UUID.randomUUID();
    } else {
      return SUBSCRIPTION_PREFIX + hostname + '-' + Instant.now().toEpochMilli();
    }
  }

  private void createSubscription() {
    try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
      subscriptionAdminClient.createSubscription(
        Subscription.newBuilder()
          .setTopic(topic.toString())
          .setName(subscriptionName)
          .setPushConfig(pushConfig)
          .setMessageRetentionDuration(
            // How long will an unprocessed message be kept - minimum 10 minutes
            com.google.protobuf.Duration.newBuilder().setSeconds(600).build()
          )
          .setExpirationPolicy(
            ExpirationPolicy.newBuilder()
              // How long will the subscription exist when no longer in use - minimum 1 day
              .setTtl(com.google.protobuf.Duration.newBuilder().setSeconds(86400).build())
              .build()
          )
          .build()
      );
    } catch (IOException e) {
      // Google libraries expects credentials json-file either as
      //   Path is stored in environment variable "GOOGLE_APPLICATION_CREDENTIALS"
      //   (https://cloud.google.com/docs/authentication/getting-started)
      // or
      //   Credentials are provided through "workload identity"
      //   (https://cloud.google.com/kubernetes-engine/docs/concepts/workload-identity)
      throw new RuntimeException(
        "Unable to initialize Google Pubsub-updater: System.getenv('GOOGLE_APPLICATION_CREDENTIALS') = " +
        System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
      );
    }
  }

  private void deleteSubscription() {
    try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
      LOG.info("Deleting subscription {}", subscriptionName);
      subscriptionAdminClient.deleteSubscription(subscriptionName);
      LOG.info(
        "Subscription deleted {} - time since startup: {}",
        subscriptionName,
        DurationUtils.durationToStr(Duration.between(startTime, Instant.now()))
      );
    } catch (IOException e) {
      LOG.error("Could not delete subscription {}", subscriptionName);
    } catch (NotFoundException nfe) {
      LOG.info("Subscription {} not found, ignoring deletion request", subscriptionName);
    }
  }

  /**
   * Decode the xml-encoded message payload into an optional SIRI ServiceDelivery.
   */
  private Optional<ServiceDelivery> serviceDelivery(ByteString data) {
    Siri siri;
    try {
      siri = SiriXml.parseXml(data.toStringUtf8());
    } catch (XMLStreamException | JAXBException e) {
      throw new RuntimeException(e);
    }
    return Optional.ofNullable(siri.getServiceDelivery());
  }

  /**
   * Fetch the backlog of messages and apply the changes to the transit model.
   * Block until the backlog is applied.
   */
  private void initializeData() {
    if (dataInitializationUrl != null) {
      LOG.info("Fetching initial data from {}", dataInitializationUrl);
      final long t1 = System.currentTimeMillis();
      ByteString value = fetchInitialData();
      final long t2 = System.currentTimeMillis();
      LOG.info(
        "Fetching initial data - finished after {} ms, got {}",
        (t2 - t1),
        FileSizeToTextConverter.fileSizeToString(value.size())
      );
      serviceDelivery(value)
        .map(serviceDeliveryConsumer)
        .ifPresent(future -> {
          try {
            future.get();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          } catch (ExecutionException e) {
            throw new RuntimeException(e);
          }
        });

      LOG.info(
        "Pubsub updater initialized after {} ms: [messages: {},  updates: {}, total size: {}, time since startup: {}]",
        (System.currentTimeMillis() - t2),
        MESSAGE_COUNTER.get(),
        UPDATE_COUNTER.get(),
        FileSizeToTextConverter.fileSizeToString(SIZE_COUNTER.get()),
        getTimeSinceStartupString()
      );
    }
  }

  /**
   * Fetch the backlog of messages over HTTP from the configured data initialization URL.
   */
  private ByteString fetchInitialData() {
    try (OtpHttpClientFactory otpHttpClientFactory = new OtpHttpClientFactory()) {
      var otpHttpClient = otpHttpClientFactory.create(LOG);
      return otpHttpClient.getAndMap(
        dataInitializationUrl,
        initialGetDataTimeout,
        Map.of("Content-Type", "application/xml"),
        ByteString::readFrom
      );
    }
  }

  /**
   * Shut down the PubSub subscriber at server shutdown.
   */
  private void addShutdownHook() {
    ApplicationShutdownSupport.addShutdownHook("siri-et-google-pubsub-shutdown", () -> {
      if (subscriber != null) {
        LOG.info("Stopping SIRI-ET PubSub subscriber '{}'.", subscriptionName);
        subscriber.stopAsync();
      }
      deleteSubscription();
    });
  }

  private String getTimeSinceStartupString() {
    return DurationUtils.durationToStr(Duration.between(startTime, Instant.now()));
  }

  /**
   * Message receiver callback that consumes messages from the PubSub subscription.
   */
  class EstimatedTimetableMessageReceiver implements MessageReceiver {

    @Override
    public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
      Optional<ServiceDelivery> serviceDelivery = serviceDelivery(message.getData());
      serviceDelivery.ifPresent(sd -> {
        logPubsubMessage(sd);
        serviceDeliveryConsumer.apply(sd);
      });

      // Ack only after all work for the message is complete.
      consumer.ack();
    }
  }

  private void logPubsubMessage(ServiceDelivery serviceDelivery) {
    int numberOfUpdatedTrips = 0;
    try {
      numberOfUpdatedTrips = serviceDelivery
        .getEstimatedTimetableDeliveries()
        .getFirst()
        .getEstimatedJourneyVersionFrames()
        .getFirst()
        .getEstimatedVehicleJourneies()
        .size();
    } catch (Exception e) {
      //ignore
    }
    long numberOfUpdates = UPDATE_COUNTER.addAndGet(numberOfUpdatedTrips);
    long numberOfMessages = MESSAGE_COUNTER.incrementAndGet();

    if (numberOfMessages % 1000 == 0) {
      LOG.info(
        "Pubsub stats: [messages: {}, updates: {}, total size: {}, current delay {} ms, time since startup: {}]",
        numberOfMessages,
        numberOfUpdates,
        FileSizeToTextConverter.fileSizeToString(SIZE_COUNTER.get()),
        Duration.between(
          serviceDelivery.getResponseTimestamp().toInstant(),
          Instant.now()
        ).toMillis(),
        getTimeSinceStartupString()
      );
    }
  }
}
