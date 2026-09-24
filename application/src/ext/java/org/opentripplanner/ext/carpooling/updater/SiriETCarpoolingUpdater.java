package org.opentripplanner.ext.carpooling.updater;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripVertexResolver;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripWithVertices;
import org.opentripplanner.updater.TransitRealTimeUpdateContext;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.WriteDomain;
import org.opentripplanner.updater.support.siri.SiriFileLoader;
import org.opentripplanner.updater.support.siri.SiriHttpLoader;
import org.opentripplanner.updater.support.siri.SiriLoader;
import org.opentripplanner.updater.trip.siri.updater.DefaultSiriETUpdaterParameters;
import org.opentripplanner.updater.trip.siri.updater.EstimatedTimetableSource;
import org.opentripplanner.updater.trip.siri.updater.SiriETHttpTripUpdateSource;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.ServiceDelivery;

/**
 * Polls carpool driver trips from a SIRI-ET HTTP source and maintains them in the
 * {@link CarpoolingRepository}. Each trip's route points are resolved to permanent, car-reachable
 * street vertices, and its corridor built, before insertion.
 * <p>
 * A poll only parses the delivery: cancellations are applied at once, and new or changed trips are
 * handed to a {@link CarpoolTripResolutionQueue} that resolves them on a background thread. The
 * updater is therefore primed, and the instance ready, once the first poll has been read, however
 * many trips the feed holds; trips become routable one by one as their resolution completes.
 */
public class SiriETCarpoolingUpdater extends PollingGraphUpdater<TransitRealTimeUpdateContext> {

  private static final Logger LOG = LoggerFactory.getLogger(SiriETCarpoolingUpdater.class);

  /**
   * How long a carpool trip is kept after its latest end time before it is purged. The SIRI-ET
   * source is not guaranteed to send an explicit cancellation once a journey has completed, so
   * completed trips are removed once their latest end time is further in the past than this
   * duration. This keeps instances that run for a long time from accumulating trips that can no
   * longer be routed.
   */
  private static final Duration TRIP_EXPIRY = Duration.ofDays(2);

  private final EstimatedTimetableSource updateSource;

  private final CarpoolingRepository repository;
  private final CarpoolTripVertexResolver vertexResolver;
  private final CarpoolSiriMapper mapper;

  /**
   * Trips whose route-point geometry failed to resolve, keyed by trip id, so an unresolvable
   * journey re-delivered with unchanged geometry skips re-linking, re-probing, and re-logging. The
   * stored trip supplies the geometry to compare against and the end time that drives expiry (swept
   * on the same expiry as stored trips).
   */
  private final Map<FeedScopedId, CarpoolTrip> failedResolutions = new ConcurrentHashMap<>();
  private final CarpoolTripResolutionQueue resolutionQueue;
  private final int maxTrips;
  private int rejectedThisPoll;

  @Nullable
  private final ExecutorService ownedExecutor;

  /**
   * @param maxTrips the most trips the instance holds, over all feeds; each updater checks the
   *        total and drops the new trips that would exceed it
   */
  public SiriETCarpoolingUpdater(
    DefaultSiriETUpdaterParameters config,
    CarpoolingRepository repository,
    CarpoolTripVertexResolver vertexResolver,
    int maxTrips
  ) {
    this(config, repository, vertexResolver, resolverThread(config.feedId()), maxTrips);
  }

  /**
   * @param resolutionExecutor runs the trip resolutions handed over by the polls; the production
   *        constructor uses one daemon thread, tests may run them inline
   * @param maxTrips the most trips the instance holds, over all feeds
   */
  SiriETCarpoolingUpdater(
    DefaultSiriETUpdaterParameters config,
    CarpoolingRepository repository,
    CarpoolTripVertexResolver vertexResolver,
    Executor resolutionExecutor,
    int maxTrips
  ) {
    super(config);
    if (maxTrips < 1) {
      throw new IllegalArgumentException("maxTrips must be positive");
    }
    this.maxTrips = maxTrips;
    this.updateSource = new SiriETHttpTripUpdateSource(config, siriLoader(config));
    this.repository = repository;
    this.vertexResolver = vertexResolver;
    this.blockReadinessUntilInitialized = config.blockReadinessUntilInitialized();
    this.ownedExecutor = resolutionExecutor instanceof ExecutorService owned ? owned : null;
    this.resolutionQueue = new CarpoolTripResolutionQueue(
      resolutionExecutor,
      this::resolveVertices,
      repository
    );
    LOG.info("Creating SIRI-ET updater running every {}: {}", pollingPeriod(), updateSource);
    this.mapper = new CarpoolSiriMapper(config.feedId());
  }

  private static ExecutorService resolverThread(String feedId) {
    return Executors.newSingleThreadExecutor(task -> {
      var thread = new Thread(task, "carpool-trip-resolver-" + feedId);
      thread.setDaemon(true);
      return thread;
    });
  }

  @Override
  public void teardown() {
    if (ownedExecutor != null) {
      ownedExecutor.shutdownNow();
    }
  }

  /** Trips handed over by the polls whose resolution has not completed yet. */
  public int pendingResolutions() {
    return resolutionQueue.pending();
  }

  @Override
  public WriteDomain<TransitRealTimeUpdateContext> writeDomain() {
    return WriteDomain.TRANSIT;
  }

  /**
   * Repeatedly makes blocking calls to an UpdateStreamer to retrieve carpooling trip updates.
   */
  @Override
  public void runPolling() {
    boolean moreData;
    do {
      moreData = fetchAndProcessUpdates();
    } while (moreData);
    removeExpiredTrips();
    int pending = resolutionQueue.pending();
    if (pending > 0) {
      LOG.info("{} carpool trips are queued for resolution", pending);
    }
    if (rejectedThisPoll > 0) {
      LOG.warn(
        "Rejected {} new carpool trips: this instance holds the maximum of {} trips",
        rejectedThisPoll,
        maxTrips
      );
      rejectedThisPoll = 0;
    }
  }

  /**
   * Purges trips that ended more than {@link #TRIP_EXPIRY} ago, so completed trips are removed even
   * when the source stops updating them. Cached resolution failures are swept on the same expiry.
   */
  private void removeExpiredTrips() {
    var now = Instant.now();
    repository.removeExpiredTrips(now, TRIP_EXPIRY);
    var cutoff = now.minus(TRIP_EXPIRY);
    failedResolutions
      .values()
      .removeIf(failed -> failed.latestEndTime().toInstant().isBefore(cutoff));
  }

  /**
   * Fetches updates from the source and processes them.
   *
   * @return true if there is more data available to fetch
   */
  private boolean fetchAndProcessUpdates() {
    var updates = updateSource.getUpdates();
    if (updates.isEmpty()) {
      return false;
    }

    ServiceDelivery serviceDelivery = updates.get().getServiceDelivery();
    processEstimatedTimetableDeliveries(serviceDelivery.getEstimatedTimetableDeliveries());
    return Boolean.TRUE.equals(serviceDelivery.isMoreData());
  }

  /**
   * Processes a list of estimated timetable deliveries.
   *
   * @param deliveries the list of estimated timetable deliveries, may be null
   */
  private void processEstimatedTimetableDeliveries(
    List<EstimatedTimetableDeliveryStructure> deliveries
  ) {
    if (deliveries == null || deliveries.isEmpty()) {
      return;
    }

    for (EstimatedTimetableDeliveryStructure delivery : deliveries) {
      var frames = delivery.getEstimatedJourneyVersionFrames();
      for (var frame : frames) {
        var estimatedVehicleJourneys = frame.getEstimatedVehicleJourneies();

        if (estimatedVehicleJourneys == null || estimatedVehicleJourneys.isEmpty()) {
          LOG.debug("Received an empty EstimatedJourneyVersionFrame, skipping");
          continue;
        }

        estimatedVehicleJourneys.forEach(this::processEstimatedVehicleJourney);
      }
    }
  }

  /**
   * Maps a journey to a carpool trip and queues it for resolution and insertion (see
   * {@link CarpoolTripResolutionQueue}). Removes the trip instead, at once, when the journey is
   * cancelled or has fewer than 2 non-cancelled calls; a trip that later fails to resolve is
   * removed by the queue.
   */
  void processEstimatedVehicleJourney(EstimatedVehicleJourney estimatedVehicleJourney) {
    try {
      FeedScopedId tripId = mapper.tripId(estimatedVehicleJourney);
      if (Boolean.TRUE.equals(estimatedVehicleJourney.isCancellation())) {
        remove(tripId);
        return;
      }
      var carpoolTrip = mapper.mapSiriToCarpoolTrip(estimatedVehicleJourney);
      if (carpoolTrip == null) {
        remove(tripId);
        return;
      }
      if (isUnchanged(tripId, carpoolTrip)) {
        return;
      }
      if (isFullFor(tripId)) {
        rejectedThisPoll++;
        LOG.debug("Rejected new carpool trip {}: the repository is full", tripId);
        return;
      }
      resolutionQueue.submit(carpoolTrip);
    } catch (Exception e) {
      LOG.info(
        "Failed to process EstimatedVehicleJourney {}",
        estimatedVehicleJourney.getEstimatedVehicleJourneyCode(),
        e
      );
    }
  }

  /**
   * Whether this delivery repeats what is already queued for the trip or, with nothing queued, what
   * is held. A source that re-sends its whole feed on every poll then costs one comparison per trip
   * instead of a resolution, and does not touch the trip limit.
   */
  private boolean isUnchanged(FeedScopedId tripId, CarpoolTrip delivered) {
    var pendingVersion = resolutionQueue.pendingVersion(tripId);
    if (pendingVersion != null) {
      return pendingVersion.sameAs(delivered);
    }
    var held = repository.getCarpoolTrip(tripId);
    return held != null && held.trip().sameAs(delivered);
  }

  /**
   * Whether a journey for this trip id has to be dropped because the instance is full: the trip is
   * neither held nor queued, and the held trips plus the queued ones not held yet reach the limit.
   * Held and queued trips already have their slot and may always be updated.
   */
  private boolean isFullFor(FeedScopedId tripId) {
    if (
      repository.getCarpoolTrip(tripId) != null || resolutionQueue.pendingVersion(tripId) != null
    ) {
      return false;
    }
    return repository.getCarpoolTrips().size() + resolutionQueue.pendingNew() >= maxTrips;
  }

  private void remove(FeedScopedId tripId) {
    resolutionQueue.cancel(tripId);
    repository.removeCarpoolTrip(tripId);
  }

  private static boolean sameDeviationBudgets(CarpoolTrip a, CarpoolTrip b) {
    if (a.stops().size() != b.stops().size()) {
      return false;
    }
    for (int i = 0; i < a.stops().size(); i++) {
      if (!a.stops().get(i).getDeviationBudget().equals(b.stops().get(i).getDeviationBudget())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Resolves the trip's route points to permanent vertices, or {@code null} if any cannot be
   * resolved. Both outcomes are memoized on the route-point geometry: an unchanged geometry reuses
   * the stored vertices or skips a known failure without re-resolving. A first failure is logged; a
   * resolution that throws is memoized as failed too, with its stack trace. Runs on the resolution
   * queue's thread.
   */
  @Nullable
  private CarpoolTripWithVertices resolveVertices(CarpoolTrip trip) {
    var existing = repository.getCarpoolTrip(trip.getId());
    if (existing != null && existing.trip().routePoints().equals(trip.routePoints())) {
      var reused = new CarpoolTripWithVertices(trip, existing.vertices());
      // The corridor also depends on the stops' deviation budgets: keep it while those are
      // unchanged too, otherwise recompute it from the reused vertices.
      return existing.corridor() != null && sameDeviationBudgets(existing.trip(), trip)
        ? reused.withCorridor(existing.corridor())
        : vertexResolver.withCorridor(reused);
    }
    var failed = failedResolutions.get(trip.getId());
    if (failed != null && failed.routePoints().equals(trip.routePoints())) {
      LOG.debug(
        "Skipping carpool trip {}: route-point geometry is unchanged since resolution failed",
        trip.getId()
      );
      return null;
    }
    CarpoolTripWithVertices resolved;
    try {
      resolved = vertexResolver.resolve(trip);
    } catch (RuntimeException e) {
      LOG.warn("Dropping carpool trip {}: route-point resolution failed", trip.getId(), e);
      failedResolutions.put(trip.getId(), trip);
      return null;
    }
    if (resolved == null) {
      LOG.warn(
        "Dropping carpool trip {}: a route point has no car-reachable street vertex",
        trip.getId()
      );
      failedResolutions.put(trip.getId(), trip);
    }
    return resolved;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(SiriETCarpoolingUpdater.class)
      .addStr("source", updateSource.toString())
      .addDuration("frequency", pollingPeriod())
      .toString();
  }

  private static SiriLoader siriLoader(DefaultSiriETUpdaterParameters config) {
    // Load real-time updates from a file.
    if (SiriFileLoader.matchesUrl(config.url())) {
      return new SiriFileLoader(config.url());
    }
    return new SiriHttpLoader(
      config.url(),
      config.timeout(),
      config.httpRequestHeaders(),
      config.previewInterval()
    );
  }
}
