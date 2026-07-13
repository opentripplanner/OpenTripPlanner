package org.opentripplanner.ext.carpooling.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCarpoolingRepository implements CarpoolingRepository {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultCarpoolingRepository.class);

  /**
   * Minimum time between expiry sweeps. Trips expire on a multi-day timescale, so scanning the
   * repository on every poll is wasteful; sweeping at most this often keeps the work negligible
   * regardless of how short — or how many — the polling feeds are.
   */
  private static final Duration SWEEP_INTERVAL = Duration.ofHours(1);

  private final Map<FeedScopedId, CarpoolTrip> trips = new ConcurrentHashMap<>();

  /**
   * Outcome of routing each trip's baseline, memoized across requests and tagged with the geometry
   * it was routed against. Kept in lockstep with {@link #trips}: an entry is dropped whenever the
   * corresponding trip's route points change or the trip leaves the repository, and a geometry
   * mismatch is ignored on read. See {@link #cachedBaselineRouting}.
   */
  private final Map<FeedScopedId, CacheEntry> baselineRouting = new ConcurrentHashMap<>();

  /** The earliest instant at which the next expiry sweep is allowed to run. */
  private final AtomicReference<Instant> nextSweep = new AtomicReference<>(Instant.MIN);

  @Override
  public Collection<CarpoolTrip> getCarpoolTrips() {
    return trips.values();
  }

  @Override
  public void upsertCarpoolTrip(CarpoolTrip trip) {
    CarpoolTrip existingTrip = trips.put(trip.getId(), trip);
    // A read already validates the cached entry against the trip's geometry, so correctness does
    // not depend on this drop; it just promptly frees an entry whose route points changed instead
    // of letting it linger until the trip is removed or expires. A budget- or time-only update
    // keeps the same route points, so the entry survives and is reused.
    if (existingTrip == null || !existingTrip.routePoints().equals(trip.routePoints())) {
      baselineRouting.remove(trip.getId());
    }
    if (existingTrip != null) {
      LOG.debug("Updated carpool trip {} with {} stops", trip.getId(), trip.stops().size());
    } else {
      LOG.debug("Added new carpool trip {} with {} stops", trip.getId(), trip.stops().size());
    }
  }

  @Override
  public void removeCarpoolTrip(FeedScopedId id) {
    CarpoolTrip removed = trips.remove(id);
    baselineRouting.remove(id);
    if (removed != null) {
      LOG.debug("Removed carpool trip {}", id);
    } else {
      LOG.debug("Tried to remove unknown carpool trip {}", id);
    }
  }

  @Override
  public int removeExpiredTrips(Instant now, Duration expiry) {
    Instant allowedAt = nextSweep.get();
    if (now.isBefore(allowedAt) || !nextSweep.compareAndSet(allowedAt, now.plus(SWEEP_INTERVAL))) {
      return 0;
    }

    Instant expiryThreshold = now.minus(expiry);
    int removed = 0;
    for (CarpoolTrip trip : trips.values()) {
      if (
        trip.latestEndTime().toInstant().isBefore(expiryThreshold) &&
        trips.remove(trip.getId(), trip)
      ) {
        baselineRouting.remove(trip.getId());
        removed++;
      }
    }
    if (removed > 0) {
      LOG.debug("Removed {} carpool trips that ended before {}", removed, expiryThreshold);
    }
    return removed;
  }

  @Override
  @Nullable
  public CachedBaselineRouting cachedBaselineRouting(CarpoolTrip trip) {
    CacheEntry entry = baselineRouting.get(trip.getId());
    if (entry == null || !entry.routePoints().equals(trip.routePoints())) {
      return null;
    }
    Duration[] legDurations = entry.legDurations();
    return new CachedBaselineRouting(legDurations == null ? null : legDurations.clone());
  }

  @Override
  public void cacheBaselineRouting(CarpoolTrip trip, @Nullable Duration[] legDurations) {
    // routePoints() already returns a fresh immutable snapshot, so it is safe to store directly.
    baselineRouting.put(
      trip.getId(),
      new CacheEntry(trip.routePoints(), legDurations == null ? null : legDurations.clone())
    );
  }

  /**
   * A cached baseline-routing outcome together with the route-point geometry it was computed for,
   * so a trip whose geometry changed is treated as a miss on read. A {@code null}
   * {@code legDurations} marks the baseline as unroutable.
   */
  private record CacheEntry(List<WgsCoordinate> routePoints, @Nullable Duration[] legDurations) {}
}
