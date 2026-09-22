package org.opentripplanner.ext.carpooling.updater;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingParameters;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTripResolver;
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
 * street vertices before insertion.
 */
public class SiriETCarpoolingUpdater extends PollingGraphUpdater<TransitRealTimeUpdateContext> {

  private static final Logger LOG = LoggerFactory.getLogger(SiriETCarpoolingUpdater.class);

  private final Duration tripExpiry;

  private final EstimatedTimetableSource updateSource;

  private final CarpoolingRepository repository;
  private final RoutableCarpoolTripResolver tripResolver;
  private final CarpoolSiriMapper mapper;

  /**
   * Trips whose route points could not be resolved or whose corridor could not be computed, keyed
   * by trip id, so an unresolvable journey re-delivered with unchanged geometry skips re-linking,
   * re-probing, and re-logging. The stored trip supplies the geometry to compare against and the
   * end time that drives expiry (swept on the same expiry as stored trips).
   */
  private final Map<FeedScopedId, CarpoolTrip> failedResolutions = new HashMap<>();

  /**
   * The feed's live trips, in the version delivered last, until the trip is removed or expires:
   * what the repository holds of this feed, plus the trips that could not be resolved. The trip
   * limit counts them.
   */
  private final Map<FeedScopedId, CarpoolTrip> liveTrips = new HashMap<>();

  private final int maxTrips;

  private int rejectedThisPoll;

  public SiriETCarpoolingUpdater(
    DefaultSiriETUpdaterParameters config,
    CarpoolingRepository repository,
    RoutableCarpoolTripResolver tripResolver,
    CarpoolingParameters parameters
  ) {
    super(config);
    this.maxTrips = parameters.maxTrips();
    this.tripExpiry = parameters.tripExpiry();
    this.updateSource = new SiriETHttpTripUpdateSource(config, siriLoader(config));
    this.repository = repository;
    this.tripResolver = tripResolver;
    this.blockReadinessUntilInitialized = config.blockReadinessUntilInitialized();

    LOG.info("Creating SIRI-ET updater running every {}: {}", pollingPeriod(), updateSource);

    this.mapper = new CarpoolSiriMapper(config.feedId(), parameters.maxTripDuration());
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
    if (rejectedThisPoll > 0) {
      LOG.warn(
        "Rejected {} new carpool trips: this feed has the maximum of {} trips",
        rejectedThisPoll,
        maxTrips
      );
      rejectedThisPoll = 0;
    }
  }

  /**
   * Purges trips that ended more than the trip expiry ago, so completed trips are removed even when
   * the source stops updating them. Cached resolution failures and the live trips are swept on the
   * same expiry.
   */
  private void removeExpiredTrips() {
    var now = Instant.now();
    repository.removeExpiredTrips(now, tripExpiry);
    var cutoff = now.minus(tripExpiry);
    failedResolutions
      .values()
      .removeIf(failed -> failed.latestEndTime().toInstant().isBefore(cutoff));
    liveTrips.values().removeIf(trip -> trip.latestEndTime().toInstant().isBefore(cutoff));
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
   * Maps a journey to a carpool trip, resolves its route points, and upserts the result. Removes
   * the trip instead when the journey is cancelled, has fewer than 2 non-cancelled calls, or fails
   * to resolve. A new trip is dropped when the feed already has as many live trips as it may.
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
      if (isFullFor(tripId)) {
        rejectedThisPoll++;
        LOG.debug("Rejected new carpool trip {}: the feed is full", tripId);
        return;
      }
      liveTrips.put(tripId, carpoolTrip);
      var routableTrip = resolveTrip(carpoolTrip);
      if (routableTrip == null) {
        repository.removeCarpoolTrip(tripId);
        return;
      }
      repository.upsertCarpoolTrip(routableTrip);
    } catch (Exception e) {
      LOG.info(
        "Failed to process EstimatedVehicleJourney {}",
        estimatedVehicleJourney.getEstimatedVehicleJourneyCode(),
        e
      );
    }
  }

  private void remove(FeedScopedId tripId) {
    liveTrips.remove(tripId);
    repository.removeCarpoolTrip(tripId);
  }

  /**
   * Whether a journey for this trip id has to be dropped because the feed is full: the trip is not
   * one of the feed's live trips, and the feed already has as many as it may. A live trip already
   * has its slot and may always be updated.
   */
  private boolean isFullFor(FeedScopedId tripId) {
    return !liveTrips.containsKey(tripId) && liveTrips.size() >= maxTrips;
  }

  /**
   * Resolves the trip's route points to permanent vertices and computes its corridor, or
   * {@code null} if a point cannot be resolved or the baseline cannot be routed. Both outcomes are
   * memoized on the route-point geometry: an unchanged geometry reuses the stored vertices or skips
   * a known failure without re-resolving. A first failure is logged; a resolution that throws is
   * memoized as failed too, with its stack trace.
   */
  @Nullable
  private RoutableCarpoolTrip resolveTrip(CarpoolTrip trip) {
    var failed = failedResolutions.get(trip.getId());
    if (failed != null && failed.routePoints().equals(trip.routePoints())) {
      LOG.debug(
        "Skipping carpool trip {}: route-point geometry is unchanged since resolution failed",
        trip.getId()
      );
      return null;
    }
    var existing = repository.getCarpoolTrip(trip.getId());
    boolean sameRoutePoints =
      existing != null && existing.trip().routePoints().equals(trip.routePoints());
    // The corridor also depends on the stops' deviation budgets.
    if (sameRoutePoints && sameDeviationBudgets(existing.trip(), trip)) {
      return new RoutableCarpoolTrip(trip, existing.vertices(), existing.corridor());
    }
    var tripOnSameRoute = storedTripOnTheSameRoute(trip);
    if (tripOnSameRoute != null) {
      return new RoutableCarpoolTrip(trip, tripOnSameRoute.vertices(), tripOnSameRoute.corridor());
    }
    RoutableCarpoolTrip resolved;
    try {
      resolved = sameRoutePoints
        ? tripResolver.resolve(trip, existing.vertices())
        : tripResolver.resolve(trip);
    } catch (RuntimeException e) {
      LOG.warn(
        "Dropping carpool trip {}: resolving its route points or computing its corridor failed",
        trip.getId(),
        e
      );
      failedResolutions.put(trip.getId(), trip);
      return null;
    }
    if (resolved == null) {
      LOG.warn(
        "Dropping carpool trip {}: a route point has no car-reachable street vertex, or a leg " +
          "cannot be routed",
        trip.getId()
      );
      failedResolutions.put(trip.getId(), trip);
    }
    return resolved;
  }

  /**
   * A stored trip, of any feed, with the same route points and deviation budgets; {@code null} if
   * there is none.
   */
  @Nullable
  private RoutableCarpoolTrip storedTripOnTheSameRoute(CarpoolTrip trip) {
    for (var stored : repository.getCarpoolTrips()) {
      if (
        stored.trip().routePoints().equals(trip.routePoints()) &&
        sameDeviationBudgets(stored.trip(), trip)
      ) {
        return stored;
      }
    }
    return null;
  }

  private static boolean sameDeviationBudgets(CarpoolTrip a, CarpoolTrip b) {
    for (int i = 0; i < a.stops().size(); i++) {
      if (!a.stops().get(i).getDeviationBudget().equals(b.stops().get(i).getDeviationBudget())) {
        return false;
      }
    }
    return true;
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
