package org.opentripplanner.ext.carpooling.updater;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
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

  private final String feedId;

  /**
   * The feed's trips, in the version delivered last, whose route points could not be resolved or
   * whose corridor could not be computed, keyed by trip id. They are not in the repository, but
   * count towards the trip limit, and an unresolvable journey re-delivered with unchanged geometry
   * skips re-linking, re-probing, and re-logging. Swept on the same expiry as stored trips.
   */
  private final Map<FeedScopedId, CarpoolTrip> failedResolutions = new HashMap<>();

  /**
   * When the source recorded the version that removed each trip no longer in the repository
   * (cancelled, or left with fewer than two calls), keyed by trip id. A removed trip leaves nothing
   * to compare a later version against, and the source may still deliver an older, active version
   * of it afterwards — a proxy with several instances delivers in whatever order its instances were
   * polled — so the removal's {@code RecordedAtTime} is kept as a tombstone until the trip would
   * have expired anyway. Only removals that carry a {@code RecordedAtTime} leave one: without it
   * there is nothing to order against. An unresolvable version needs no tombstone, since it stays
   * in {@link #failedResolutions} with its own {@code RecordedAtTime}.
   */
  private final Map<FeedScopedId, ZonedDateTime> removedTrips = new HashMap<>();

  private final int maxTrips;

  private final int maxPagesPerPoll;

  private int rejectedThisPoll;

  public SiriETCarpoolingUpdater(
    DefaultSiriETUpdaterParameters config,
    CarpoolingRepository repository,
    RoutableCarpoolTripResolver tripResolver,
    CarpoolingParameters parameters
  ) {
    super(config);
    this.feedId = config.feedId();
    this.maxTrips = parameters.maxTrips();
    this.maxPagesPerPoll = parameters.maxPagesPerPoll();
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
    int pages = 0;
    boolean moreData;
    do {
      moreData = fetchAndProcessUpdates();
      pages++;
    } while (moreData && (runOnlyOnce() || pages < maxPagesPerPoll));
    if (moreData) {
      LOG.warn(
        "Read the maximum of {} pages from {} in one poll; the rest follows on the next poll",
        maxPagesPerPoll,
        updateSource
      );
    }
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
   * the source stops updating them. Cached resolution failures and removal tombstones are swept on
   * the same expiry, a tombstone by the time its removal was recorded, which is the latest the trip
   * could still have been active.
   */
  private void removeExpiredTrips() {
    removeExpiredTrips(Instant.now());
  }

  void removeExpiredTrips(Instant now) {
    repository.removeExpiredTrips(now, tripExpiry);
    var cutoff = now.minus(tripExpiry);
    failedResolutions
      .values()
      .removeIf(failed -> failed.latestEndTime().toInstant().isBefore(cutoff));
    removedTrips.values().removeIf(recordedAt -> recordedAt.toInstant().isBefore(cutoff));
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
   * Processes a list of estimated timetable deliveries. A journey that carries no
   * {@code RecordedAtTime} of its own is stamped with its frame's before processing: SIRI requires
   * the frame's and makes the journey's optional, to be given only when it differs from the
   * frame's, so the frame's is the journey's when the journey has none.
   *
   * @param deliveries the list of estimated timetable deliveries, may be null
   */
  void processEstimatedTimetableDeliveries(List<EstimatedTimetableDeliveryStructure> deliveries) {
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

        var frameRecordedAt = frame.getRecordedAtTime();
        for (var journey : estimatedVehicleJourneys) {
          if (journey.getRecordedAtTime() == null) {
            journey.setRecordedAtTime(frameRecordedAt);
          }
          processEstimatedVehicleJourney(journey);
        }
      }
    }
  }

  /**
   * Maps a journey to a carpool trip, resolves its route points, and upserts the result. Removes
   * the trip instead when the journey is cancelled, has fewer than 2 non-cancelled calls, or fails
   * to resolve. A delivery that repeats the trip's last one is skipped, and a new trip is dropped
   * when the feed already has as many live trips as it may.
   * <p>
   * A journey recorded by the source before the version already applied — stored, failed to
   * resolve, or removed and remembered as a tombstone — is ignored before any of that work: it is
   * an older version arriving late, and applying it would briefly revert the trip. Versions
   * recorded at the same time, and journeys or applied versions without a {@code RecordedAtTime},
   * are applied as before, so a source that does not stamp its journeys, or stamps them without
   * advancing the stamp on every change, is never silenced by this check. Timestamps are compared
   * only with each other, never with the local clock.
   */
  void processEstimatedVehicleJourney(EstimatedVehicleJourney estimatedVehicleJourney) {
    try {
      FeedScopedId tripId = mapper.tripId(estimatedVehicleJourney);
      var recordedAt = estimatedVehicleJourney.getRecordedAtTime();
      if (isOlderThanApplied(tripId, recordedAt)) {
        LOG.debug(
          "Ignoring carpool trip {} recorded at {}: a newer version has already been applied",
          tripId,
          recordedAt
        );
        return;
      }
      if (Boolean.TRUE.equals(estimatedVehicleJourney.isCancellation())) {
        remove(tripId, recordedAt);
        return;
      }
      var carpoolTrip = mapper.mapSiriToCarpoolTrip(estimatedVehicleJourney);
      if (carpoolTrip == null) {
        remove(tripId, recordedAt);
        return;
      }
      if (isUnchanged(tripId, carpoolTrip)) {
        return;
      }
      if (isFullFor(tripId)) {
        rejectedThisPoll++;
        LOG.debug("Rejected new carpool trip {}: the feed is full", tripId);
        return;
      }
      var routableTrip = resolveTrip(carpoolTrip);
      if (routableTrip == null) {
        failedResolutions.put(tripId, carpoolTrip);
        repository.removeCarpoolTrip(tripId);
        removedTrips.remove(tripId);
        return;
      }
      failedResolutions.remove(tripId);
      repository.upsertCarpoolTrip(routableTrip);
      removedTrips.remove(tripId);
    } catch (Exception e) {
      LOG.info(
        "Failed to process EstimatedVehicleJourney {}",
        estimatedVehicleJourney.getEstimatedVehicleJourneyCode(),
        e
      );
    }
  }

  /** Whether this delivery repeats the trip's last delivery. */
  private boolean isUnchanged(FeedScopedId tripId, CarpoolTrip delivered) {
    var last = lastDelivery(tripId);
    return last != null && last.sameAs(delivered);
  }

  /**
   * Whether the source recorded this version before the one already applied for the trip: its last
   * delivery's {@code RecordedAtTime}, or the tombstone's if the trip was removed. Unknown on
   * either side means not older.
   */
  private boolean isOlderThanApplied(FeedScopedId tripId, @Nullable ZonedDateTime recordedAt) {
    if (recordedAt == null) {
      return false;
    }
    var last = lastDelivery(tripId);
    var applied = last != null ? last.recordedAt() : removedTrips.get(tripId);
    return applied != null && recordedAt.isBefore(applied);
  }

  /**
   * The trip's last delivery, from the repository or, if it could not be resolved, from the failed
   * resolutions; {@code null} if the feed does not have the trip.
   */
  @Nullable
  private CarpoolTrip lastDelivery(FeedScopedId tripId) {
    var stored = repository.getCarpoolTrip(tripId);
    return stored != null ? stored.trip() : failedResolutions.get(tripId);
  }

  /**
   * Removes the trip and, when the removing version carries a {@code RecordedAtTime}, leaves a
   * tombstone so an older active version delivered later does not resurrect it.
   */
  private void remove(FeedScopedId tripId, @Nullable ZonedDateTime recordedAt) {
    failedResolutions.remove(tripId);
    repository.removeCarpoolTrip(tripId);
    if (recordedAt != null) {
      removedTrips.put(tripId, recordedAt);
    }
  }

  /**
   * Whether a journey for this trip id has to be dropped because the feed is full
   */
  private boolean isFullFor(FeedScopedId tripId) {
    return (
      lastDelivery(tripId) == null &&
      repository.countCarpoolTrips(feedId) + failedResolutions.size() >= maxTrips
    );
  }

  /**
   * Resolves the trip's route points to permanent vertices and computes its corridor, or
   * {@code null} if a point cannot be resolved or the baseline cannot be routed.
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
      return new RoutableCarpoolTrip(trip, existing.corridor());
    }
    var tripOnSameRoute = storedTripOnTheSameRoute(trip);
    if (tripOnSameRoute != null) {
      return new RoutableCarpoolTrip(trip, tripOnSameRoute.corridor());
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
      return null;
    }
    if (resolved == null) {
      LOG.warn(
        "Dropping carpool trip {}: a route point has no car-reachable street vertex, or a leg " +
          "cannot be routed",
        trip.getId()
      );
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
    for (int leg = 0; leg < a.stops().size() - 1; leg++) {
      if (!a.legDeviationBudget(leg).equals(b.legDeviationBudget(leg))) {
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
