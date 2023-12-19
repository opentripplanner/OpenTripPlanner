package org.opentripplanner.ext.siri.updater;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.pubsub.v1.ExpirationPolicy;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.entur.protobuf.mapper.SiriMapper;
import org.opentripplanner.ext.siri.EntityResolver;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.framework.application.ApplicationShutdownSupport;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.retry.OtpRetry;
import org.opentripplanner.framework.retry.OtpRetryBuilder;
import org.opentripplanner.framework.text.FileSizeToTextConverter;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.Siri;
import uk.org.siri.www.siri.SiriType;

/**
 * This class starts a Google PubSub subscription
 * <p>
 * NOTE: - Path to Google credentials (.json-file) MUST exist in environment-variable
 * "GOOGLE_APPLICATION_CREDENTIALS" as described here:
 * <a href="https://cloud.google.com/docs/authentication/getting-started">ServiceAccount need access to
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
 *   "topicName": "protobuf.estimated_timetables",                                      // Google Cloud Pubsub topic
 *   "dataInitializationUrl": "http://server/realtime/protobuf/et"  // Optional URL used to initialize OTP with all existing data
 * </pre>
 */
public class SiriETGooglePubsubUpdater implements GraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(SiriETGooglePubsubUpdater.class);

  private static final AtomicLong MESSAGE_COUNTER = new AtomicLong(0);
  private static final AtomicLong UPDATE_COUNTER = new AtomicLong(0);
  private static final AtomicLong SIZE_COUNTER = new AtomicLong(0);
  private static final String SUBSCRIPTION_PREFIX = "siri-et-";

  private static final int RETRY_MAX_ATTEMPTS = Integer.MAX_VALUE;
  private static final Duration RETRY_INITIAL_DELAY = Duration.ofSeconds(1);
  private static final int RETRY_BACKOFF = 2;

  /**
   * The URL used to fetch all initial updates
   */
  private final URI dataInitializationUrl;
  /**
   * The ID for the static feed to which these TripUpdates are applied
   */
  private final String feedId;
  /**
   * The number of seconds to wait before reconnecting after a failed connection.
   */
  private final java.time.Duration reconnectPeriod;

  /**
   * For larger deployments it sometimes takes more than the default 30 seconds to fetch data, if so
   * this parameter can be increased.
   */
  private final java.time.Duration initialGetDataTimeout;

  private final String subscriptionName;
  private final ProjectTopicName topic;
  private final Subscriber subscriber;
  private final PushConfig pushConfig;
  private final String configRef;
  private final SiriTimetableSnapshotSource snapshotSource;
  private final SiriFuzzyTripMatcher fuzzyTripMatcher;
  private final Instant startTime = Instant.now();
  private final Consumer<UpdateResult> recordMetrics;
  private final EntityResolver entityResolver;

  private final OtpRetry retry;

  /**
   * Parent update manager. Is used to execute graph writer runnables.
   */
  private WriteToGraphCallback saveResultOnGraph;

  private volatile boolean primed;

  public SiriETGooglePubsubUpdater(
    SiriETGooglePubsubUpdaterParameters config,
    TransitModel transitModel,
    SiriTimetableSnapshotSource timetableSnapshot
  ) {
    this.configRef = config.configRef();

    // URL that responds to HTTP GET which returns all initial data in protobuf-format. Will be
    // called once to initialize real-time-data. All updates will be received from Google Cloud
    // Pubsub
    this.dataInitializationUrl = URI.create(config.dataInitializationUrl());
    this.feedId = config.feedId();
    this.reconnectPeriod = config.reconnectPeriod();
    this.initialGetDataTimeout = config.initialGetDataTimeout();
    this.snapshotSource = timetableSnapshot;

    // set subscriber
    String subscriptionId = buildSubscriptionId();
    String subscriptionProjectName = config.subscriptionProjectName();
    String topicProjectName = config.topicProjectName();

    String topicName = config.topicName();

    subscriptionName =
      ProjectSubscriptionName.of(subscriptionProjectName, subscriptionId).toString();
    subscriber =
      Subscriber.newBuilder(subscriptionName, new EstimatedTimetableMessageReceiver()).build();
    this.topic = ProjectTopicName.of(topicProjectName, topicName);
    this.pushConfig = PushConfig.getDefaultInstance();
    TransitService transitService = new DefaultTransitService(transitModel);
    this.entityResolver = new EntityResolver(transitService, feedId);
    this.fuzzyTripMatcher =
      config.fuzzyTripMatching() ? SiriFuzzyTripMatcher.of(transitService) : null;
    recordMetrics = TripUpdateMetrics.streaming(config);

    addShutdownHook();
    retry =
      new OtpRetryBuilder()
        .withName("SIRI-ET Google PubSub Updater setup")
        .withMaxAttempts(RETRY_MAX_ATTEMPTS)
        .withInitialRetryInterval(RETRY_INITIAL_DELAY)
        .withBackoffMultiplier(RETRY_BACKOFF)
        .build();
  }

  @Override
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
  }

  @Override
  public void run() {
    try {
      LOG.info("Creating subscription {}", subscriptionName);
      retry.execute(this::createSubscription);
      LOG.info("Created subscription {}", subscriptionName);

      // Retrying until data is initialized successfully
      retry.execute(this::initializeData);

      while (true) {
        try {
          subscriber.startAsync().awaitRunning();
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
    return this.primed;
  }

  @Override
  public String getConfigRef() {
    return configRef;
  }

  private void addShutdownHook() {
    ApplicationShutdownSupport.addShutdownHook(
      "siri-et-google-pubsub-shutdown",
      () -> {
        if (subscriber != null) {
          LOG.info("Stopping SIRI-ET PubSub subscriber '{}'.", subscriptionName);
          subscriber.stopAsync();
        }
        deleteSubscription();
      }
    );
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
        Subscription
          .newBuilder()
          .setTopic(topic.toString())
          .setName(subscriptionName)
          .setPushConfig(pushConfig)
          .setMessageRetentionDuration(
            // How long will an unprocessed message be kept - minimum 10 minutes
            com.google.protobuf.Duration.newBuilder().setSeconds(600).build()
          )
          .setExpirationPolicy(
            ExpirationPolicy
              .newBuilder()
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

  private String getTimeSinceStartupString() {
    return DurationUtils.durationToStr(Duration.between(startTime, Instant.now()));
  }

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
      processSiriData(value);
      primed = true;
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

  private ByteString fetchInitialData() {
    try (OtpHttpClient otpHttpClient = new OtpHttpClient()) {
      return otpHttpClient.getAndMap(
        dataInitializationUrl,
        initialGetDataTimeout,
        Map.of("Content-Type", "application/x-protobuf"),
        ByteString::readFrom
      );
    }
  }

  private void processSiriData(ByteString data) {
    Siri siri;
    try {
      SIZE_COUNTER.addAndGet(data.size());
      final SiriType siriType = SiriType.parseFrom(data);
      siri = SiriMapper.mapToJaxb(siriType);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }

    if (siri.getServiceDelivery() != null) {
      // Handle trip updates via graph writer runnable
      List<EstimatedTimetableDeliveryStructure> estimatedTimetableDeliveries = siri
        .getServiceDelivery()
        .getEstimatedTimetableDeliveries();

      int numberOfUpdatedTrips = 0;
      try {
        numberOfUpdatedTrips =
          estimatedTimetableDeliveries
            .get(0)
            .getEstimatedJourneyVersionFrames()
            .get(0)
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
          Duration
            .between(siri.getServiceDelivery().getResponseTimestamp().toInstant(), Instant.now())
            .toMillis(),
          getTimeSinceStartupString()
        );
      }

      var f = saveResultOnGraph.execute((graph, transitModel) -> {
        var results = snapshotSource.applyEstimatedTimetable(
          fuzzyTripMatcher,
          entityResolver,
          feedId,
          false,
          estimatedTimetableDeliveries
        );

        recordMetrics.accept(results);
      });

      if (!isPrimed()) {
        try {
          f.get();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        } catch (ExecutionException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  class EstimatedTimetableMessageReceiver implements MessageReceiver {

    @Override
    public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
      processSiriData(message.getData());
      // Ack only after all work for the message is complete.
      consumer.ack();
    }
  }
}
