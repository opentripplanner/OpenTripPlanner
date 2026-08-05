package org.opentripplanner.ext.carpooling.updater;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripVertexResolver;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripWithVertices;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
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
 * street vertices before insertion.
 */
public class SiriETCarpoolingUpdater extends PollingGraphUpdater {

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
  private final Map<FeedScopedId, CarpoolTrip> failedResolutions = new HashMap<>();

  public SiriETCarpoolingUpdater(
    DefaultSiriETUpdaterParameters config,
    CarpoolingRepository repository,
    CarpoolTripVertexResolver vertexResolver
  ) {
    super(config);
    this.updateSource = new SiriETHttpTripUpdateSource(config, siriLoader(config));
    this.repository = repository;
    this.vertexResolver = vertexResolver;
    this.blockReadinessUntilInitialized = config.blockReadinessUntilInitialized();

    LOG.info("Creating SIRI-ET updater running every {}: {}", pollingPeriod(), updateSource);

    this.mapper = new CarpoolSiriMapper(config.feedId());
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
          LOG.warn("Received an empty EstimatedJourneyVersionFrame, skipping");
          continue;
        }

        estimatedVehicleJourneys.forEach(this::processEstimatedVehicleJourney);
      }
    }
  }

  /**
   * Maps a journey to a carpool trip, resolves its route points, and upserts the result. Removes the
   * trip instead when the journey is cancelled, has fewer than 2 non-cancelled calls, or fails to
   * resolve.
   */
  void processEstimatedVehicleJourney(EstimatedVehicleJourney estimatedVehicleJourney) {
    try {
      FeedScopedId tripId = mapper.tripId(estimatedVehicleJourney);
      if (Boolean.TRUE.equals(estimatedVehicleJourney.isCancellation())) {
        repository.removeCarpoolTrip(tripId);
        return;
      }
      var carpoolTrip = mapper.mapSiriToCarpoolTrip(estimatedVehicleJourney);
      if (carpoolTrip == null) {
        repository.removeCarpoolTrip(tripId);
        return;
      }
      var tripWithVertices = resolveVertices(carpoolTrip);
      if (tripWithVertices == null) {
        repository.removeCarpoolTrip(tripId);
        return;
      }
      repository.upsertCarpoolTrip(tripWithVertices);
    } catch (Exception e) {
      LOG.warn("Failed to process EstimatedVehicleJourney", e);
    }
  }

  /**
   * Resolves the trip's route points to permanent vertices, or {@code null} if any cannot be
   * resolved. Both outcomes are memoized on the route-point geometry: an unchanged geometry reuses
   * the stored vertices or skips a known failure without re-resolving. A first failure is logged; a
   * resolution that throws is memoized as failed too, with its stack trace.
   */
  @Nullable
  private CarpoolTripWithVertices resolveVertices(CarpoolTrip trip) {
    var existing = repository.getCarpoolTrip(trip.getId());
    if (existing != null && existing.trip().routePoints().equals(trip.routePoints())) {
      return new CarpoolTripWithVertices(trip, existing.vertices());
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
